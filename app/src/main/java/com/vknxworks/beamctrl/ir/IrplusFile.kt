package com.vknxworks.beamctrl.ir

import java.util.Locale

/**
 * Reads irplus remotes (`.irplus` / `.xml`, the format of the irplus app and its irplus-codes collection): an
 * `<irplus>` root with one `<device manufacturer=… model=… format=…>` holding `<button label=…>code</button>`
 * elements. The code formats with an unambiguous meaning are read:
 *
 * - `PRONTO_HEX`: a Pronto hex code.
 * - `WINLIRC_RAW`: on/off timings in microseconds (`frequency` attribute, or 38 kHz).
 * - `WINLIRC_NEC1`: NEC with its 32 bits written the LIRC way, most significant bit first, as a 16-bit "pre data"
 *   and a 16-bit code (`0x010E 0x03FC`).
 *
 * The other `WINLIRC_*` formats describe their timings with attributes whose exact meaning isn't documented, so
 * buttons in them are reported as skipped rather than guessed.
 */
object IrplusFile {

    sealed interface Code {
        class Pronto(val hex: String) : Code
        class Raw(val frequency: Int, val timings: List<Int>) : Code
        /** NEC as it goes over the air, LSB first: [address] and [command] are 16 bits each. */
        class Nec(val address: Int, val command: Int) : Code {
            /** Whether the second byte of each is the inverse of the first (standard NEC with 8-bit values). */
            val standard: Boolean
                get() = (address shr 8) == (address.inv() and 0xFF) && (command shr 8) == (command.inv() and 0xFF)
        }
    }

    class Button(val label: String, val code: Code)

    class Result(val manufacturer: String?, val model: String?, val buttons: List<Button>, val skipped: List<String>)

    private const val MAX_BUTTONS = 500
    private const val DEFAULT_GAP = 40_000

    fun looksLike(text: String): Boolean = text.take(4_096).contains("<irplus", ignoreCase = true)

    private val DEVICE = Regex("<device\\b([^>]*)>", RegexOption.IGNORE_CASE)
    private val BUTTON = Regex("<button\\b([^>]*)>(.*?)</button>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ATTR = Regex("([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')")
    private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val WHITESPACE = Regex("[\\s,]+")

    private fun attrs(s: String): Map<String, String> =
        ATTR.findAll(s).associate { m -> m.groupValues[1].lowercase(Locale.ROOT) to unescape(m.groups[3]?.value ?: m.groups[4]?.value.orEmpty()) }

    /** XML character references and the five named entities. */
    fun unescape(s: String): String = Regex("&(#x[0-9A-Fa-f]+|#[0-9]+|amp|lt|gt|quot|apos);").replace(s) { m ->
        val e = m.groupValues[1]
        when {
            e.startsWith("#x") -> e.substring(2).toIntOrNull(16)?.let { cp -> codePoint(cp) } ?: ""
            e.startsWith("#") -> e.substring(1).toIntOrNull()?.let { cp -> codePoint(cp) } ?: ""
            e == "amp" -> "&"
            e == "lt" -> "<"
            e == "gt" -> ">"
            e == "quot" -> "\""
            else -> "'"
        }
    }

    private fun codePoint(cp: Int): String =
        // Icon-font code points (private use areas) mean nothing outside the irplus app: they're dropped.
        if (cp in 0xE000..0xF8FF || cp >= 0xF0000 || cp !in 0x20..0x10FFFF) "" else String(Character.toChars(cp))

    fun parse(text: String): Result {
        val clean = COMMENT.replace(text, "")
        val device = DEVICE.find(clean)?.let { attrs(it.groupValues[1]) }.orEmpty()
        val format = device["format"]?.uppercase(Locale.ROOT).orEmpty()
        val frequency = device["frequency"]?.toDoubleOrNull()?.toInt()?.takeIf { it > 0 } ?: 38_000
        val buttons = ArrayList<Button>()
        val skipped = ArrayList<String>()
        for (m in BUTTON.findAll(clean)) {
            val label = attrs(m.groupValues[1])["label"]?.trim().orEmpty()
            val body = unescape(m.groupValues[2]).trim()
            if (buttons.size >= MAX_BUTTONS) { skipped += label; continue }
            val code = when (format) {
                "PRONTO_HEX" -> body.takeIf { it.isNotEmpty() }?.let { Code.Pronto(it) }
                "WINLIRC_RAW" -> raw(body, frequency)
                "WINLIRC_NEC1" -> nec1(body)
                else -> null
            }
            if (code == null) skipped += label else buttons += Button(label, code)
        }
        return Result(device["manufacturer"]?.trim()?.ifEmpty { null }, device["model"]?.trim()?.ifEmpty { null }, buttons, skipped)
    }

    private fun raw(body: String, frequency: Int): Code? {
        val t = WHITESPACE.split(body).filter { it.isNotEmpty() }.take(IrLimits.MAX_TIMINGS + 1).map { it.toIntOrNull() ?: return null }
        if (t.isEmpty() || t.any { it <= 0 }) return null
        // Ends with a pulse: give it the usual gap so repeats are kept apart.
        return Code.Raw(frequency, if (t.size % 2 == 1) t + DEFAULT_GAP else t)
    }

    private fun hexValue(s: String): Long? {
        val t = s.trim()
        val digits = if (t.startsWith("0x") || t.startsWith("0X")) t.substring(2) else return null
        return digits.toLongOrNull(16)
    }

    private fun reverse8(v: Int): Int = Integer.reverse(v and 0xFF) ushr 24

    /** "0xPPPP 0xCCCC": 32 bits, most significant first, turned into NEC's LSB-first address and command. */
    internal fun nec1(body: String): Code? {
        val parts = WHITESPACE.split(body).filter { it.isNotEmpty() }
        val word: Long = when (parts.size) {
            2 -> {
                val pre = hexValue(parts[0])?.takeIf { it in 0..0xFFFF } ?: return null
                val data = hexValue(parts[1])?.takeIf { it in 0..0xFFFF } ?: return null
                (pre shl 16) or data
            }
            1 -> hexValue(parts[0])?.takeIf { it in 0..0xFFFFFFFFL } ?: return null
            else -> return null
        }
        val b = IntArray(4) { i -> reverse8(((word shr (24 - 8 * i)) and 0xFF).toInt()) }
        return Code.Nec(b[0] or (b[1] shl 8), b[2] or (b[3] shl 8))
    }
}
