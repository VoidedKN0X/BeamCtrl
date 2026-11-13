package com.vknxworks.beamctrl.ir

import android.content.Context
import com.vknxworks.beamctrl.R
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

/** A device type in the code library, e.g. "TVs" or "Air conditioners". [name] is the library's own (English) name. */
class LibraryCategory(val index: Int, val name: String, val deviceCount: Int) {
    /** The category's name in the phone's language. */
    fun displayName(context: Context): String = NAMES[name]?.let { context.getString(it) } ?: name

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

    private companion object {
        val NAMES: Map<String, Int> = mapOf(
            "TVs" to R.string.cat_tvs,
            "Soundbars" to R.string.cat_soundbars,
            "AV receivers" to R.string.cat_av_receivers,
            "Projectors" to R.string.cat_projectors,
            "Streaming devices" to R.string.cat_streaming_devices,
            "Cable and satellite boxes" to R.string.cat_cable_and_satellite_boxes,
            "Air conditioners" to R.string.cat_air_conditioners,
            "Fans" to R.string.cat_fans,
            "Lights" to R.string.cat_lights,
            "Air purifiers" to R.string.cat_air_purifiers,
            "Bidets" to R.string.cat_bidets,
            "Blu-ray players" to R.string.cat_blu_ray_players,
            "Cameras" to R.string.cat_cameras,
            "Car head units" to R.string.cat_car_head_units,
            "Car multimedia" to R.string.cat_car_multimedia,
            "CCTV" to R.string.cat_cctv,
            "CD players" to R.string.cat_cd_players,
            "Ceiling lifts" to R.string.cat_ceiling_lifts,
            "Clocks" to R.string.cat_clocks,
            "Computers" to R.string.cat_computers,
            "Converters and switches" to R.string.cat_converters_and_switches,
            "Digital signs" to R.string.cat_digital_signs,
            "Dust collectors" to R.string.cat_dust_collectors,
            "DVB-T receivers" to R.string.cat_dvb_t_receivers,
            "DVD players" to R.string.cat_dvd_players,
            "Fireplaces" to R.string.cat_fireplaces,
            "Game consoles" to R.string.cat_game_consoles,
            "Heaters" to R.string.cat_heaters,
            "Humidifiers" to R.string.cat_humidifiers,
            "KVM switches" to R.string.cat_kvm_switches,
            "Laserdisc players" to R.string.cat_laserdisc_players,
            "MiniDisc players" to R.string.cat_minidisc_players,
            "Miscellaneous" to R.string.cat_miscellaneous,
            "Monitors" to R.string.cat_monitors,
            "Multimedia" to R.string.cat_multimedia,
            "Picture frames" to R.string.cat_picture_frames,
            "Speakers" to R.string.cat_speakers,
            "Touchscreen displays" to R.string.cat_touchscreen_displays,
            "Toys" to R.string.cat_toys,
            "TV tuners" to R.string.cat_tv_tuners,
            "Universal TV remotes" to R.string.cat_universal_tv_remotes,
            "Vacuum cleaners" to R.string.cat_vacuum_cleaners,
            "VCRs" to R.string.cat_vcrs,
            "Videoconferencing" to R.string.cat_videoconferencing,
            "Whiteboards" to R.string.cat_whiteboards,
            "Window cleaners" to R.string.cat_window_cleaners,
            "More devices" to R.string.cat_more_devices,
            "Other" to R.string.cat_other,
        )
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
                        // A damaged line is left out rather than stopping the whole library from opening.
                        val category = f[1].toIntOrNull()?.takeIf { it >= 0 }
                        val signals = f[4].toIntOrNull()?.takeIf { it >= 0 }
                        val offset = f[6].toLongOrNull()?.takeIf { it >= 0 }
                        val length = f[7].toIntOrNull()?.takeIf { it in 1..MAX_BLOB }
                        if (category != null && signals != null && offset != null && length != null) {
                            devices += LibraryDevice(
                                id = devices.size,
                                category = category,
                                brand = f[2],
                                model = f[3],
                                signalCount = signals,
                                hasPower = f[5] == "1",
                                offset = offset,
                                length = length,
                            )
                        }
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
        irRequire(device.length in 1..MAX_BLOB && device.offset >= 0, R.string.library_damaged)
        val packed = ByteArray(device.length)
        context.assets.openFd("$DIR/data.bin").use { afd ->
            irRequire(device.offset + device.length <= afd.length, R.string.library_damaged)
            FileInputStream(afd.fileDescriptor).channel.use { ch ->
                val buf = ByteBuffer.wrap(packed)
                var pos = afd.startOffset + device.offset
                while (buf.hasRemaining()) {
                    val n = ch.read(buf, pos)
                    if (n < 0) throw IrCodeException(R.string.library_damaged)
                    pos += n
                }
            }
        }
        FlipperIr.parse(inflate(packed), context.getString(R.string.button_default))
    }

    /** Every distinct TV power code in the library, most widely used first. */
    suspend fun tvPowerCodes(): List<RemoteButton> = withContext(Dispatchers.IO) {
        val text = context.assets.open("$DIR/tv_power.ir").use { it.readBytes().decodeToString() }
        // Power codes are matched to the power key by name; here every one is just a code to send.
        FlipperIr.parse(text, context.getString(R.string.button_default)).buttons.map { it.copy(role = null) }
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
                irRequire(out.size() <= MAX_TEXT, R.string.library_too_big)
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
