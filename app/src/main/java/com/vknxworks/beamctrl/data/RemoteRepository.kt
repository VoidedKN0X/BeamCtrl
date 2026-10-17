package com.vknxworks.beamctrl.data

import android.content.Context
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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** A backup of every remote, as written by "Export all remotes". */
@Serializable
data class Backup(val app: String = "BeamCtrl", val version: Int = 1, val remotes: List<Remote>)

/** Saved remotes, kept in one JSON file in the app's private storage. */
class RemoteRepository(context: Context, private val scope: CoroutineScope) {
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
            }.getOrElse { emptyList() }
            _remotes.value = loaded
            _ready.value = true
        }
    }

    fun get(id: String): Remote? = _remotes.value.firstOrNull { it.id == id }

    fun add(remote: Remote): Remote {
        val named = remote.copy(name = uniqueName(remote.name))
        change { it + named }
        return named
    }

    fun update(id: String, transform: (Remote) -> Remote) = change { list ->
        list.map { if (it.id == id) transform(it) else it }
    }

    fun delete(id: String) = change { list -> list.filterNot { it.id == id } }

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
        while ("$base $n".lowercase() in names) n++
        return "$base $n"
    }

    fun exportJson(): String = json.encodeToString(Backup.serializer(), Backup(remotes = _remotes.value))

    /** Adds the remotes in a backup, giving new ids to any that already exist here. Returns how many were added. */
    fun importJson(text: String): Int {
        val backup = json.decodeFromString(Backup.serializer(), text)
        val existing = _remotes.value.map { it.id }.toSet()
        val incoming = backup.remotes.map { r ->
            if (r.id in existing) r.copy(id = java.util.UUID.randomUUID().toString()) else r
        }
        change { it + incoming }
        return incoming.size
    }

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

    companion object {
        private val listSerializer = ListSerializer(Remote.serializer())

        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
}
