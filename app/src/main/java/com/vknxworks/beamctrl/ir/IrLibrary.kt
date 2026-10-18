package com.vknxworks.beamctrl.ir

import android.content.Context
import com.vknxworks.beamctrl.data.DeviceType
import com.vknxworks.beamctrl.data.RemoteButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.Locale
import java.util.zip.Inflater

/** A device type in the code library, e.g. "TVs" or "Air conditioners". */
class LibraryCategory(val index: Int, val name: String, val deviceCount: Int) {
    val type: DeviceType
        get() = when (name) {
            "TVs", "Monitors", "Touchscreen displays", "Universal TV remotes" -> DeviceType.TV
            "Soundbars", "Speakers", "AV receivers", "CD players", "MiniDisc players" -> DeviceType.SOUNDBAR
            "Projectors" -> DeviceType.PROJECTOR
            "Streaming devices", "Cable and satellite boxes", "DVB-T receivers", "TV tuners",
            "Blu-ray players", "DVD players", "VCRs", "Laserdisc players", "Game consoles" -> DeviceType.BOX
            "Air conditioners", "Heaters", "Fireplaces", "Air purifiers", "Humidifiers" -> DeviceType.AC
            "Fans" -> DeviceType.FAN
            "Lights" -> DeviceType.LIGHT
            else -> DeviceType.OTHER
        }
}

/** One remote in the code library. Its signals are read only when it's opened. */
class LibraryDevice(
    val id: Int,
    val category: Int,
    val brand: String,
    val model: String,
    val signalCount: Int,
    val hasPower: Boolean,
    internal val offset: Long,
    internal val length: Int,
) {
    val title: String get() = "$brand $model"
    internal val searchText: String = "$brand $model".lowercase(Locale.ROOT)
}

/**
 * The Flipper-IRDB code library that ships inside the app (packed by tools/irdb/pack_irdb.py).
 * Everything is read from the APK; nothing is downloaded.
 */
class IrLibrary(private val context: Context) {

    class Index(
        val source: String,
        val categories: List<LibraryCategory>,
        val devices: List<LibraryDevice>,
    )

    private val lock = Mutex()
    private var index: Index? = null

    /** Reads the index the first time it's needed (a few hundred kilobytes, well under a second). */
    suspend fun index(): Index = lock.withLock {
        index ?: withContext(Dispatchers.IO) { readIndex() }.also { index = it }
    }

    private fun readIndex(): Index {
        val names = mutableListOf<String>()
        val devices = ArrayList<LibraryDevice>(9_000)
        var source = ""
        context.assets.open("$DIR/index.tsv").bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val f = line.split('\t')
                when (f.firstOrNull()) {
                    "BeamCtrl-IRDB" -> source = "Flipper-IRDB ${f.getOrNull(2).orEmpty()} (${f.getOrNull(3).orEmpty()})"
                    "C" -> names += f.getOrElse(1) { "Other" }
                    "D" -> if (f.size >= 8) {
                        devices += LibraryDevice(
                            id = devices.size,
                            category = f[1].toInt(),
                            brand = f[2],
                            model = f[3],
                            signalCount = f[4].toInt(),
                            hasPower = f[5] == "1",
                            offset = f[6].toLong(),
                            length = f[7].toInt(),
                        )
                    }
                }
            }
        }
        val counts = devices.groupingBy { it.category }.eachCount()
        val categories = names.mapIndexed { i, n -> LibraryCategory(i, n, counts[i] ?: 0) }
        return Index(source, categories, devices)
    }

    suspend fun categories(): List<LibraryCategory> = index().categories.filter { it.deviceCount > 0 }

    suspend fun category(index: Int): LibraryCategory? = index().categories.getOrNull(index)

    suspend fun device(id: Int): LibraryDevice? = index().devices.getOrNull(id)

    /** Brands in a category with how many remotes each has, A to Z. */
    suspend fun brands(category: Int): List<Pair<String, Int>> = withContext(Dispatchers.Default) {
        index().devices.asSequence()
            .filter { it.category == category }
            .groupingBy { it.brand }.eachCount()
            .toList()
            .sortedBy { it.first.lowercase(Locale.ROOT) }
    }

    suspend fun devices(category: Int, brand: String): List<LibraryDevice> = withContext(Dispatchers.Default) {
        index().devices.filter { it.category == category && it.brand == brand }
    }

    /**
     * Remotes whose brand and model contain every word of [query]. Brand matches come first,
     * then the main categories (TVs, soundbars…) before the rest.
     */
    suspend fun search(query: String, limit: Int = 300): List<LibraryDevice> = withContext(Dispatchers.Default) {
        val words = query.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return@withContext emptyList()
        index().devices.asSequence()
            .filter { d -> words.all { it in d.searchText } }
            .sortedWith(
                compareBy<LibraryDevice> { !it.brand.lowercase(Locale.ROOT).startsWith(words.first()) }
                    .thenBy { it.category }
                    .thenBy { it.searchText },
            )
            .take(limit)
            .toList()
    }

    /** The device's buttons, through the same checks as an imported .ir file. */
    suspend fun load(device: LibraryDevice): FlipperIr.ImportResult = withContext(Dispatchers.IO) {
        require(device.length in 1..MAX_BLOB) { "That remote is damaged" }
        val packed = ByteArray(device.length)
        context.assets.openFd("$DIR/data.bin").use { afd ->
            require(device.offset + device.length <= afd.length) { "That remote is damaged" }
            FileInputStream(afd.fileDescriptor).channel.use { ch ->
                val buf = ByteBuffer.wrap(packed)
                var pos = afd.startOffset + device.offset
                while (buf.hasRemaining()) {
                    val n = ch.read(buf, pos)
                    if (n < 0) error("That remote is damaged")
                    pos += n
                }
            }
        }
        FlipperIr.parse(inflate(packed))
    }

    /** Every distinct TV power code in the library, most widely used first. */
    suspend fun tvPowerCodes(): List<RemoteButton> = withContext(Dispatchers.IO) {
        val text = context.assets.open("$DIR/tv_power.ir").use { it.readBytes().decodeToString() }
        // Power codes are matched to the power key by name; here every one is just a code to send.
        FlipperIr.parse(text).buttons.map { it.copy(role = null) }
    }

    private fun inflate(data: ByteArray): String {
        val inflater = Inflater(true)
        try {
            inflater.setInput(data)
            val out = ByteArrayOutputStream(data.size * 4)
            val buf = ByteArray(16 * 1024)
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                out.write(buf, 0, n)
                require(out.size() <= MAX_TEXT) { "That remote is too big" }
            }
            return out.toByteArray().decodeToString()
        } finally {
            inflater.end()
        }
    }

    companion object {
        private const val DIR = "irdb"
        private const val MAX_BLOB = 2 * 1024 * 1024
        private const val MAX_TEXT = 4 * 1024 * 1024
    }
}
