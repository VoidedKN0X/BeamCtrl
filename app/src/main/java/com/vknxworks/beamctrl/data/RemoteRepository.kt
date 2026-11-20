package com.vknxworks.beamctrl.data

import android.content.Context
import com.vknxworks.beamctrl.R
import com.vknxworks.beamctrl.data.crypto.DataKeyStore
import com.vknxworks.beamctrl.data.crypto.SealedFile
import com.vknxworks.beamctrl.ir.encode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * Saved remotes, kept in one JSON file in the app's private storage.
 *
 * The file is encrypted at rest with the app's data key ([DataKeyStore], [SealedFile]). A file from before
 * encryption is still read, and is then replaced by its encrypted copy (see [SealedFile.sealInPlace]). When
 * there is no data key to be had, the app keeps reading and writing the ordinary file, as before.
 */
class RemoteRepository(context: Context, private val scope: CoroutineScope, private val keys: DataKeyStore) {
    private val appContext: Context = context.applicationContext

    /** The name for a remote whose name is empty. */
    private val unnamed: String = context.getString(R.string.remote_unnamed)
    private val file = File(context.filesDir, FILE)
    private val writeLock = Mutex()

    private val _remotes = MutableStateFlow<List<Remote>>(emptyList())
    val remotes: StateFlow<List<Remote>> = _remotes

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _unreadable = MutableStateFlow(false)
    /**
     * The saved remotes are encrypted and can't be opened (the key in the phone's secure storage is gone or
     * changed, or the file is damaged). The file is left exactly as it is, nothing is written while this is
     * true, and the app shows what happened instead of an empty list.
     */
    val unreadable: StateFlow<Boolean> = _unreadable

    init {
        scope.launch(Dispatchers.IO) { open() }
    }

    private suspend fun open() = writeLock.withLock {
        try {
            val loaded = load(file, keys)
            if (loaded != null) _remotes.value = loaded
            _unreadable.value = loaded == null
        } finally {
            _ready.value = true
        }
    }

    /** Tries to open the saved remotes again, after [unreadable]. */
    fun retry() {
        scope.launch(Dispatchers.IO) {
            if (!_unreadable.value) return@launch
            keys.reset()
            open()
        }
    }

    /**
     * Gives up on saved remotes that can't be opened and starts empty, so a backup can be restored. With
     * [keep] the file is set aside as `remotes.json.unreadable` (and the key file as `data.key.unreadable`)
     * instead of deleted. Does nothing unless the remotes are [unreadable].
     */
    suspend fun startEmpty(keep: Boolean) = withContext(Dispatchers.IO) {
        writeLock.withLock {
            if (!_unreadable.value) return@withLock
            runCatching {
                if (keep) setAside(file) else file.delete()
                File(file.parentFile, TMP).delete()
            }
            // A key that still works stays; one that's gone makes way for a new one.
            if (keys.get(create = false) == null) keys.discard(keep)
            _remotes.value = emptyList()
            _unreadable.value = false
        }
    }

    fun get(id: String): Remote? = _remotes.value.firstOrNull { it.id == id }

    fun add(remote: Remote): Remote {
        val clean = sanitize(remote, _remotes.value.map { it.id }.toSet())
        val named = clean.copy(name = uniqueName(clean.name))
        change { it + named }
        return named
    }

    fun update(id: String, transform: (Remote) -> Remote) = change { list ->
        list.map { if (it.id == id) transform(it) else it }
    }

    fun delete(id: String) = change { list -> list.filterNot { it.id == id } }

    /** Deletes every remote ("Forgot PIN?"). */
    fun deleteAll() = change { emptyList() }

    fun markUsed(id: String) {
        // Not worth a disk write on every press: only when it changes the order on the list.
        val now = System.currentTimeMillis()
        val r = get(id) ?: return
        if (now - r.lastUsed > 60_000) update(id) { it.copy(lastUsed = now) }
    }

    fun uniqueName(base: String): String {
        val names = _remotes.value.map { it.name.lowercase() }.toSet()
        if (base.lowercase() !in names) return base
        var n = 2
        while (appContext.getString(R.string.name_numbered, base, n).lowercase() in names) n++
        return appContext.getString(R.string.name_numbered, base, n)
    }

    /** "Restore from a file": replaces every remote with [restored], already checked by [BackupFile.read]. */
    fun replaceAll(restored: List<Remote>) = change { restored }

    private fun change(transform: (List<Remote>) -> List<Remote>) {
        // Never over remotes that couldn't be opened: they stay on disk until they open again or are given up.
        if (_unreadable.value) return
        _remotes.update(transform)
        scope.launch {
            writeLock.withLock {
                withContext(Dispatchers.IO) { save() }
            }
        }
    }

    /** Writes the latest list to disk. Only called with [writeLock] held, off the main thread. */
    private fun save() {
        if (_unreadable.value) return
        // Write to a temporary file and swap it in, so a crash never leaves half a file.
        val tmp = File(file.parentFile, TMP)
        try {
            // Always write the latest list, so out-of-order writes can't save an older one.
            val plain = json.encodeToString(listSerializer, _remotes.value).toByteArray(Charsets.UTF_8)
            // Encrypted when there's a data key; the ordinary file otherwise, as before.
            val sealed = keys.get(create = true)?.let { SealedFile.sealChecked(it, plain) }
            SealedFile.writeAtomic(file, tmp, sealed ?: plain)
        } catch (e: IOException) {
            // Full or failing storage: keep the remotes in memory and the last saved file as it was; the next
            // change tries again. (The temporary file only goes while the saved file is still there: if the
            // swap itself failed halfway, it's the one complete copy, and the next start puts it back.)
            if (file.exists()) {
                tmp.delete()
            }
        }
    }

    /**
     * Makes a remote from a file safe to keep: one-line names of a sane length, a bounded number of
     * buttons, only codes the blaster can send, and ids that don't clash with [takenIds] or each other.
     */
    private fun sanitize(remote: Remote, takenIds: Set<String>): Remote {
        val buttonIds = mutableSetOf<String>()
        val buttons = remote.buttons.asSequence()
            .take(MAX_BUTTONS)
            .filter { b -> runCatching { b.code.encode() }.isSuccess }
            .map { b ->
                val id = b.id.takeIf { it.isSaneId() && it !in buttonIds } ?: UUID.randomUUID().toString()
                buttonIds += id
                b.copy(id = id, label = b.label.clean(MAX_LABEL))
            }
            .toList()
        return remote.copy(
            id = remote.id.takeIf { it.isSaneId() && it !in takenIds } ?: UUID.randomUUID().toString(),
            name = remote.name.clean(MAX_NAME).ifEmpty { unnamed },
            brand = remote.brand?.clean(MAX_NAME)?.ifEmpty { null },
            buttons = buttons,
        )
    }

    private fun String.clean(max: Int): String = filterNot { it.isISOControl() }.trim().take(max).trim()

    private fun String.isSaneId(): Boolean = length in 1..64 && all { it.isLetterOrDigit() || it == '-' }

    companion object {
        /** Bigger files than this are refused before they're read into memory. */
        const val MAX_IMPORT_BYTES = 4L * 1024 * 1024
        private const val MAX_BUTTONS = BackupFile.MAX_BUTTONS
        private const val MAX_NAME = BackupFile.MAX_NAME
        private const val MAX_LABEL = BackupFile.MAX_LABEL

        private const val FILE = "remotes.json"
        private const val TMP = "remotes.json.tmp"
        private const val ASIDE = "remotes.json.unreadable"

        private val listSerializer = ListSerializer(Remote.serializer())

        private fun decode(bytes: ByteArray): List<Remote> = json.decodeFromString(listSerializer, String(bytes, Charsets.UTF_8))

        /** Moves an unreadable file out of the way without deleting it (or an earlier one set aside). */
        private fun setAside(file: File) {
            var aside = File(file.parentFile, ASIDE)
            if (aside.exists()) aside = File(file.parentFile, "$ASIDE.${System.currentTimeMillis()}")
            file.renameTo(aside)
        }

        /** The contents of a sealed or ordinary file, or null when it's sealed and can't be opened. */
        private fun plainOf(bytes: ByteArray, keys: DataKeyStore): ByteArray? {
            if (!SealedFile.isSealed(bytes)) return bytes
            val key = keys.get(create = false) ?: return null
            return try {
                SealedFile.open(key, bytes)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * When `remotes.json` is missing, puts back a complete copy that was left next to it:
         * - `remotes.json.tmp`: the newest save, when the app died after writing it and before it took the
         *   file's place;
         * - `remotes.json.unreadable`, only when it's an encrypted file that opens: a version of BeamCtrl
         *   from before encryption sets the encrypted file aside under that name because it can't read it.
         * Only a copy that opens and reads as remotes is put back; anything else is left where it is.
         */
        private fun recoverMissing(file: File, keys: DataKeyStore) {
            val tmp = File(file.parentFile, TMP)
            val aside = File(file.parentFile, ASIDE)
            for (candidate in listOf(tmp, aside)) {
                try {
                    if (!candidate.exists()) continue
                    val bytes = candidate.readBytes()
                    if (candidate == aside && !SealedFile.isSealed(bytes)) continue
                    val plain = plainOf(bytes, keys) ?: continue
                    decode(plain)
                    if (candidate.renameTo(file)) return
                } catch (e: Exception) {
                    // Not a usable copy.
                }
            }
        }

        /**
         * Reads the saved remotes (the app's one load at start, or a retry). Null means the file is encrypted
         * and can't be opened; it is left untouched. An ordinary or opened file that doesn't read as remotes
         * is kept aside instead of being overwritten, and the list starts empty, as before. An ordinary file
         * that reads fine is then replaced by its encrypted copy; if that fails for any reason the ordinary
         * file simply stays in use.
         */
        private fun load(file: File, keys: DataKeyStore): List<Remote>? {
            if (!file.exists()) recoverMissing(file, keys)
            if (!file.exists()) return emptyList()
            val bytes = try {
                file.readBytes()
            } catch (e: Exception) {
                runCatching { setAside(file) }
                return emptyList()
            }
            val sealed = SealedFile.isSealed(bytes)
            val plain = plainOf(bytes, keys) ?: return null
            val list = try {
                decode(plain)
            } catch (e: Exception) {
                runCatching { setAside(file) }
                return emptyList()
            }
            if (!sealed) {
                try {
                    val key = keys.get(create = true)
                    if (key != null) SealedFile.sealInPlace(key, file, File(file.parentFile, TMP))
                } catch (e: Throwable) {
                    // The ordinary file stays in use.
                }
            }
            return list
        }

        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
}

/** Reads at most [maxBytes] as UTF-8 text; a longer stream fails with [FileTooBigException] instead of filling memory. */
fun InputStream.readTextCapped(maxBytes: Long = RemoteRepository.MAX_IMPORT_BYTES): String {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(16 * 1024)
    var total = 0L
    while (true) {
        val n = read(buf)
        if (n < 0) break
        total += n
        if (total > maxBytes) throw FileTooBigException()
        out.write(buf, 0, n)
    }
    return out.toByteArray().decodeToString()
}

class FileTooBigException : java.io.IOException("That file is too big")
