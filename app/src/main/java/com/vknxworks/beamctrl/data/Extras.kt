package com.vknxworks.beamctrl.data

import com.vknxworks.beamctrl.data.crypto.DataKeyStore
import com.vknxworks.beamctrl.data.crypto.SealedFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException
import java.util.UUID

/** One step of a macro: press [buttonId] on [remoteId], then wait [delayMs] before the next step. */
@Serializable
data class MacroStep(val remoteId: String, val buttonId: String, val delayMs: Int = 500)

/** A named series of button presses on one or more remotes ("Watch TV": TV on, soundbar on, input). */
@Serializable
data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val steps: List<MacroStep> = emptyList(),
    val created: Long = System.currentTimeMillis(),
)

/** A sleep timer: press [buttonId] on [remoteId] at [at] (milliseconds since 1970). */
@Serializable
data class SleepTimer(val remoteId: String, val buttonId: String, val at: Long)

/** Everything BeamCtrl keeps besides the remotes themselves. */
@Serializable
data class Extras(val macros: List<Macro> = emptyList(), val sleep: SleepTimer? = null)

object MacroLimits {
    const val MAX_MACROS = 200
    const val MAX_STEPS = 50
    const val MAX_DELAY_MS = 10_000
    val DELAYS_MS = listOf(0, 300, 500, 1_000, 2_000, 3_000, 5_000, 10_000)
}

/**
 * Macros and the sleep timer, kept in `extras.json` in the app's private storage and encrypted at rest exactly
 * like the remotes ([RemoteRepository]): sealed with the same data key, an ordinary file from before a key existed
 * is read and then sealed in place, and a sealed file that can't be opened is never overwritten ([unreadable]).
 */
class ExtrasRepository(filesDir: File, private val scope: CoroutineScope, private val keys: DataKeyStore) {
    private val file = File(filesDir, FILE)
    private val writeLock = Mutex()

    private val _extras = MutableStateFlow(Extras())
    val extras: StateFlow<Extras> = _extras

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _unreadable = MutableStateFlow(false)
    /** The file is encrypted and can't be opened: it's left as it is and nothing is saved. */
    val unreadable: StateFlow<Boolean> = _unreadable

    init {
        scope.launch(Dispatchers.IO) { open() }
    }

    private suspend fun open() = writeLock.withLock {
        try {
            val loaded = load()
            if (loaded != null) _extras.value = loaded
            _unreadable.value = loaded == null
        } finally {
            _ready.value = true
        }
    }

    /** Tries again after [unreadable] (the remotes' "Try again" also calls this). */
    fun retry() {
        scope.launch(Dispatchers.IO) {
            if (_unreadable.value) open()
        }
    }

    /** "Start empty" / "Forgot PIN?": gives up on a file that can't be opened. [keep] sets it aside instead of deleting it. */
    suspend fun startEmpty(keep: Boolean) = withContext(Dispatchers.IO) {
        writeLock.withLock {
            if (!_unreadable.value) return@withLock
            runCatching { if (keep) setAside() else file.delete() }
            _extras.value = Extras()
            _unreadable.value = false
        }
    }

    fun macro(id: String): Macro? = _extras.value.macros.firstOrNull { it.id == id }

    fun addMacro(macro: Macro) = change { it.copy(macros = (it.macros + macro).take(MacroLimits.MAX_MACROS)) }

    fun updateMacro(id: String, transform: (Macro) -> Macro) = change { e ->
        e.copy(macros = e.macros.map { if (it.id == id) transform(it) else it })
    }

    fun deleteMacro(id: String) = change { e -> e.copy(macros = e.macros.filterNot { it.id == id }) }

    fun setSleep(timer: SleepTimer?) = change { it.copy(sleep = timer) }

    /** Removes steps for remotes or buttons that are gone, and the sleep timer of a remote that's gone. */
    fun dropMissing(remotes: List<Remote>) {
        val buttons = remotes.associate { r -> r.id to r.buttons.map { it.id }.toSet() }
        val e = _extras.value
        val macros = e.macros.map { m -> m.copy(steps = m.steps.filter { s -> buttons[s.remoteId]?.contains(s.buttonId) == true }) }
        val sleep = e.sleep?.takeIf { s -> buttons[s.remoteId]?.contains(s.buttonId) == true }
        if (macros != e.macros || sleep != e.sleep) change { it.copy(macros = macros, sleep = sleep) }
    }

    /** "Restore from a file": the macros from a backup replace every macro here. */
    fun replaceMacros(macros: List<Macro>) = change { it.copy(macros = macros) }

    /** "Forgot PIN?": everything goes. */
    fun deleteAll() = change { Extras() }

    private fun change(transform: (Extras) -> Extras) {
        if (_unreadable.value) return
        _extras.update(transform)
        scope.launch {
            writeLock.withLock { withContext(Dispatchers.IO) { save() } }
        }
    }

    private fun save() {
        if (_unreadable.value) return
        val tmp = File(file.parentFile, TMP)
        try {
            val plain = RemoteRepository.json.encodeToString(Extras.serializer(), _extras.value).toByteArray(Charsets.UTF_8)
            val sealed = keys.get(create = true)?.let { SealedFile.sealChecked(it, plain) }
            SealedFile.writeAtomic(file, tmp, sealed ?: plain)
        } catch (e: IOException) {
            if (file.exists()) tmp.delete()
        }
    }

    private fun setAside() {
        var aside = File(file.parentFile, ASIDE)
        if (aside.exists()) aside = File(file.parentFile, "$ASIDE.${System.currentTimeMillis()}")
        file.renameTo(aside)
    }

    private fun decode(bytes: ByteArray): Extras = RemoteRepository.json.decodeFromString(Extras.serializer(), String(bytes, Charsets.UTF_8))

    /** Null when the file is sealed and can't be opened (left untouched). */
    private fun load(): Extras? {
        val tmp = File(file.parentFile, TMP)
        if (!file.exists() && tmp.exists()) {
            // The newest save, when the app died before it took the file's place: only if it reads.
            runCatching {
                val bytes = tmp.readBytes()
                val plain = if (SealedFile.isSealed(bytes)) keys.get(create = false)?.let { SealedFile.open(it, bytes) } else bytes
                if (plain != null) { decode(plain); tmp.renameTo(file) }
            }
        }
        if (!file.exists()) return Extras()
        val bytes = try {
            file.readBytes()
        } catch (e: Exception) {
            runCatching { setAside() }
            return Extras()
        }
        val sealed = SealedFile.isSealed(bytes)
        val plain = if (!sealed) bytes else {
            val key = keys.get(create = false) ?: return null
            try { SealedFile.open(key, bytes) } catch (e: Exception) { return null }
        }
        val extras = try {
            decode(plain)
        } catch (e: Exception) {
            runCatching { setAside() }
            return Extras()
        }
        if (!sealed) {
            try {
                keys.get(create = true)?.let { SealedFile.sealInPlace(it, file, tmp) }
            } catch (e: Throwable) {
                // The ordinary file stays in use.
            }
        }
        return extras
    }

    companion object {
        private const val FILE = "extras.json"
        private const val TMP = "extras.json.tmp"
        private const val ASIDE = "extras.json.unreadable"
    }
}
