package com.vknxworks.beamctrl.ir

import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.data.RemoteButton
import java.util.Locale

/**
 * Reads and writes Flipper Zero ".ir" signal files, the most common way IR codes are shared
 * (for example the Flipper-IRDB collection).
 */
object FlipperIr {

    class ImportResult(val buttons: List<RemoteButton>, val skipped: List<String>)

    fun parse(text: String, fallbackName: String): ImportResult {
        val buttons = mutableListOf<RemoteButton>()
        val skipped = mutableListOf<String>()
        val usedRoles = mutableSetOf<KeyRole>()

        val blocks = mutableListOf<Map<String, String>>()
        var current = linkedMapOf<String, String>()
        fun flush() {
            if (current.containsKey("name")) blocks.add(current)
            current = linkedMapOf()
        }
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#") -> flush()
                line.isEmpty() -> Unit
                else -> {
                    val i = line.indexOf(':')
                    if (i > 0) {
                        val key = line.substring(0, i).trim().lowercase(Locale.ROOT)
                        // A new name starts a new signal even if the file left out the "#" separator.
                        if (key == "name" && current.containsKey("name")) flush()
                        current[key] = line.substring(i + 1).trim()
                    }
                }
            }
        }
        flush()

        for ((index, b) in blocks.withIndex()) {
            // A real remote has a few dozen signals; stop at a sane number instead of filling memory.
            if (index >= MAX_SIGNALS) {
                skipped.add(b["name"].orEmpty())
                continue
            }
            val name = b["name"].orEmpty().ifBlank { fallbackName }
            val code = runCatching { toCode(b) }.getOrNull()
            if (code == null) {
                skipped.add(name)
                continue
            }
            // Only the first button for each role goes on the layout; the rest stay in "More buttons".
            val role = roleFor(name)?.takeIf { usedRoles.add(it) }
            buttons.add(RemoteButton(label = prettyName(name), role = role, code = code))
        }
        return ImportResult(buttons, skipped)
    }

    private fun bytesLe(value: String?): Int {
        val parts = value.orEmpty().split(Regex("\\s+")).filter { it.isNotEmpty() }
        var out = 0
        parts.take(4).forEachIndexed { i, p -> out = out or ((p.toInt(16) and 0xFF) shl (8 * i)) }
        return out
    }

    private fun toCode(b: Map<String, String>): IrCode? {
        return when (b["type"]?.lowercase(Locale.ROOT)) {
            "raw" -> {
                val freq = b["frequency"]?.toDoubleOrNull()?.takeIf { it.isFinite() }?.toInt() ?: 38_000
                val timings = b["data"].orEmpty().split(Regex("\\s+")).asSequence()
                    .mapNotNull { it.toIntOrNull() }.filter { it > 0 }
                    .take(IrLimits.MAX_TIMINGS + 1).toList()
                // Only keep codes the blaster can actually send (frequency, length and timing limits).
                if (timings.isEmpty()) null else IrCode.Raw(freq, timings).also { it.encode() }
            }
            "parsed" -> {
                val a = bytesLe(b["address"])
                val c = bytesLe(b["command"])
                when (b["protocol"]?.lowercase(Locale.ROOT)) {
                    "nec" -> IrCode.Nec(a and 0xFF, c and 0xFF)
                    "necext" -> IrCode.Nec(a and 0xFFFF, c and 0xFFFF, extended = true)
                    "samsung32" -> IrCode.Samsung32(a and 0xFF, c and 0xFF)
                    "sirc" -> IrCode.Sirc(12, a and 0x1F, c and 0x7F)
                    "sirc15" -> IrCode.Sirc(15, a and 0xFF, c and 0x7F)
                    "sirc20" -> IrCode.Sirc(20, a and 0x1FFF, c and 0x7F)
                    "rc5" -> IrCode.Rc5(a and 0x1F, c and 0x3F)
                    "rc5x" -> IrCode.Rc5(a and 0x1F, (c and 0x7F).let { if (it < 64) it + 64 else it })
                    "rc6" -> IrCode.Rc6(a and 0xFF, c and 0xFF)
                    else -> null
                }
            }
            else -> null
        }
    }

    /** Signals read from one file; the rest are reported as skipped. */
    const val MAX_SIGNALS = 500

    private fun prettyName(name: String): String =
        name.replace('_', ' ').filterNot { it.isISOControl() }.trim().take(MAX_LABEL).replaceFirstChar { it.titlecase(Locale.getDefault()) }

    private val roleNames: Map<String, KeyRole> = buildMap<String, KeyRole> {
        fun alias(role: KeyRole, vararg names: String) = names.forEach { this[it] = role }
        alias(KeyRole.POWER, "power", "pwr", "poweron", "onoff", "power toggle")
        alias(KeyRole.INPUT, "input", "source", "src", "av", "tvav")
        alias(KeyRole.MUTE, "mute")
        alias(KeyRole.VOL_UP, "volup", "volumeup", "vol+", "volplus")
        alias(KeyRole.VOL_DOWN, "voldn", "voldown", "volumedown", "vol-", "volminus")
        alias(KeyRole.CH_UP, "chnext", "chup", "channelup", "ch+", "chplus", "pageup")
        alias(KeyRole.CH_DOWN, "chprev", "chdown", "chdn", "channeldown", "ch-", "chminus", "pagedown")
        alias(KeyRole.UP, "up", "arrowup")
        alias(KeyRole.DOWN, "down", "arrowdown")
        alias(KeyRole.LEFT, "left", "arrowleft")
        alias(KeyRole.RIGHT, "right", "arrowright")
        alias(KeyRole.OK, "ok", "enter", "select", "center")
        alias(KeyRole.BACK, "back", "return")
        alias(KeyRole.HOME, "home", "smarthub")
        alias(KeyRole.MENU, "menu", "settings", "setup")
        alias(KeyRole.EXIT, "exit")
        alias(KeyRole.INFO, "info", "display")
        alias(KeyRole.GUIDE, "guide", "epg")
        alias(KeyRole.PLAY, "play")
        alias(KeyRole.PAUSE, "pause")
        alias(KeyRole.PLAY_PAUSE, "playpause")
        alias(KeyRole.STOP, "stop")
        alias(KeyRole.REWIND, "rewind", "rew", "rwd")
        alias(KeyRole.FAST_FORWARD, "fastforward", "ff", "fwd", "forward")
        (0..9).forEach { alias(KeyRole.digit(it), "$it", "num$it", "digit$it", "key$it") }
    }

    private fun roleFor(name: String): KeyRole? {
        val n = name.lowercase(Locale.ROOT).trim()
        roleNames[n]?.let { return it }
        return roleNames[n.filter { it.isLetterOrDigit() || it == '+' || it == '-' }]
            ?: roleNames[n.filter { it.isLetterOrDigit() }]
    }

    // ---------- Export ----------

    private const val MAX_LABEL = 60

    /** Keeps a name on one line, so a label can't add its own lines (fake signals) to an exported file. */
    private fun oneLine(text: String): String = text.filterNot { it.isISOControl() }.trim()

    private fun le(value: Int, bytes: Int = 4): String =
        (0 until bytes).joinToString(" ") { hex((value shr (8 * it)) and 0xFF, 2) }

    fun write(remote: Remote): String = buildString {
        append("Filetype: IR signals file\n")
        append("Version: 1\n")
        append("# Exported from BeamCtrl: ${oneLine(remote.name)}\n")
        for (b in remote.buttons) {
            append("#\n")
            append("name: ${oneLine(b.label).replace(' ', '_').ifEmpty { "Button" }}\n")
            val c = b.code
            fun parsed(protocol: String, address: Int, command: Int) {
                append("type: parsed\n")
                append("protocol: $protocol\n")
                append("address: ${le(address)}\n")
                append("command: ${le(command)}\n")
            }
            when (c) {
                is IrCode.Nec -> if (c.extended || c.address > 0xFF || c.command > 0xFF) parsed("NECext", c.address, c.command) else parsed("NEC", c.address, c.command)
                is IrCode.Samsung32 -> parsed("Samsung32", c.address, c.command)
                is IrCode.Sirc -> parsed(if (c.bits == 12) "SIRC" else "SIRC${c.bits}", c.address, c.command)
                is IrCode.Rc5 -> parsed(if (c.command >= 64) "RC5X" else "RC5", c.address, c.command)
                is IrCode.Rc6 -> parsed("RC6", c.address, c.command)
                is IrCode.Pronto, is IrCode.Raw -> {
                    val signal = c.encode()
                    append("type: raw\n")
                    append("frequency: ${signal.frequency}\n")
                    append("duty_cycle: 0.330000\n")
                    append("data: ${signal.once.joinToString(" ")}\n")
                }
            }
        }
    }
}
