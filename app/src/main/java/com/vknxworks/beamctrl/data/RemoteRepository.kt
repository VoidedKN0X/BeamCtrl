package com.vknxworks.beamctrl.data

import android.content.Context
import com.vknxworks.beamctrl.R
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
import java.io.InputStream
import java.util.UUID

/** Saved remotes, kept in one JSON file in the app's private storage. */
class RemoteRepository(context: Context, private val scope: CoroutineScope) {
    private val appContext: Context = context.applicationContext

    /** The name for a remote whose name is empty. */
    private val unnamed: String = context.getString(R.string.remote_unnamed)
    private val file = File(context.filesDir, "remotes.json")
    private val writeLock = Mutex()

    private val _remotes = MutableStateFlow<List<Remote>>(emptyList())
    val remotes: StateFlow<List<Remote>> = _remotes

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    init {
        scope.launch(Dispatchers.IO) {
            val loaded = runCatching {
                if (file.exists()) json.decodeFromString(listSerializer, file.readText()) else emptyList()
            }.getOrElse {
                // Keep an unreadable file aside instead of overwriting it with the next save.
                runCatching { file.renameTo(File(file.parentFile, "remotes.json.unreadable")) }
                emptyList()
            }
            _remotes.value = loaded
            _ready.value = true
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
        _remotes.update(transform)
        scope.launch {
            writeLock.withLock {
                withContext(Dispatchers.IO) {
                    // Write to a temporary file and swap it in, so a crash never leaves half a file.
                    val tmp = File(file.parentFile, "remotes.json.tmp")
                    // Always write the latest list, so out-of-order writes can't save an older one.
                    tmp.writeText(json.encodeToString(listSerializer, _remotes.value))
                    if (!tmp.renameTo(file)) {
                        file.writeText(tmp.readText())
                        tmp.delete()
                    }
                }
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

        private val listSerializer = ListSerializer(Remote.serializer())

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
