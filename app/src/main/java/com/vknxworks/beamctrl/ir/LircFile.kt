package com.vknxworks.beamctrl.ir

import java.util.Locale

/**
 * Reads LIRC remote definitions (`lircd.conf`, the format of the LIRC remotes database and WinLIRC), as described
 * in the lircd.conf(5) manual page. Two kinds of remote are understood:
 *
 * - **Space or pulse encoded** (`SPACE_ENC`, `PULSE_ENC`, or no encoding flag): every code is turned into on/off
 *   timings the way LIRC's own transmitter builds them: header, `plead`, pre data, `pre`, the code, `post`, post
 *   data, `ptrail`, `foot`, then the gap (`CONST_LENGTH`: the gap is the length of the whole frame). Bits go out
 *   most significant first unless the remote has `REVERSE`. `min_repeat` frames are added after the first.
 * - **Raw codes** (`RAW_CODES`, a `begin raw_codes` section): the timings as written, followed by the gap.
 *
 * Bi-phase and other encodings (`RC5`, `RC6`, `SHIFT_ENC`, `RCMM`, `GRUNDIG`, `BO`, `SERIAL`, `XMP`) are reported
 * as skipped rather than guessed. Only the first value of a code line is used (later values are toggle variants).
 */
object LircFile {

    /** One signal: its name as in the file, carrier frequency in Hz, and on/off timings in microseconds. */
    class Signal(val name: String, val frequency: Int, val timings: List<Int>)

    class Result(val remoteName: String?, val signals: List<Signal>, val skipped: List<String>)

    /** Signals read from one file; the rest are reported as skipped. Mirrors [FlipperIr.MAX_SIGNALS]. */
    const val MAX_SIGNALS = 500
    private const val MAX_BITS = 64
    private const val DEFAULT_GAP = 40_000
    private const val DEFAULT_FREQUENCY = 38_000

    /** Whether [text] looks like a LIRC file (has a `begin remote` line). */
    fun looksLike(text: String): Boolean =
        text.lineSequence().take(2_000).any { it.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").startsWith("begin remote") }

    private class Remote {
        var name: String? = null
        val params = HashMap<String, List<String>>()
        val codes = ArrayList<Pair<String, String>>()
        val raw = ArrayList<Pair<String, MutableList<Int>>>()
    }

    fun parse(text: String): Result {
        val remotes = ArrayList<Remote>()
        var current: Remote? = null
        var section: String? = null
        for (rawLine in text.lineSequence()) {
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) continue
            val words = line.split(Regex("\\s+"))
            val first = words[0].lowercase(Locale.ROOT)
            if (first == "begin" && words.size >= 2) {
                when (words[1].lowercase(Locale.ROOT)) {
                    "remote" -> current = Remote().also { remotes += it }
                    "codes" -> section = "codes"
                    "raw_codes" -> section = "raw"
                }
                continue
            }
            if (first == "end" && words.size >= 2) {
                when (words[1].lowercase(Locale.ROOT)) {
                    "remote" -> { current = null; section = null }
                    "codes", "raw_codes" -> section = null
                }
                continue
            }
            val r = current ?: continue
            when (section) {
                "codes" -> if (words.size >= 2 && r.codes.size <= MAX_SIGNALS) r.codes += words[0] to words[1]
                "raw" -> {
                    if (first == "name" && words.size >= 2) {
                        if (r.raw.size <= MAX_SIGNALS) r.raw += words[1] to ArrayList()
                    } else {
                        val target = r.raw.lastOrNull()?.second ?: continue
                        for (w in words) {
                            if (target.size > IrLimits.MAX_TIMINGS) break
                            w.toIntOrNull()?.let { target += it }
                        }
                    }
                }
                else -> if (first == "name" && words.size >= 2) r.name = words.drop(1).joinToString(" ") else r.params[first] = words.drop(1)
            }
        }

        val signals = ArrayList<Signal>()
        val skipped = ArrayList<String>()
        for (r in remotes) {
            val flags = r.params["flags"].orEmpty().joinToString("|").uppercase(Locale.ROOT).split('|').map { it.trim() }.toSet()
            val frequency = r.params["frequency"]?.firstOrNull()?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_FREQUENCY
            val gap = r.params["gap"]?.firstOrNull()?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_GAP
            for ((name, timings) in r.raw) {
                if (signals.size >= MAX_SIGNALS) { skipped += name; continue }
                val t = timings.filter { it > 0 }
                if (t.isEmpty()) { skipped += name; continue }
                // A raw code ends with a pulse; the gap follows it.
                val withGap = if (t.size % 2 == 1) t + gap else t
                signals += Signal(name, frequency, withGap)
            }
            if (r.codes.isEmpty()) continue
            val unsupported = flags.any { it in UNSUPPORTED }
            for ((name, value) in r.codes) {
                if (signals.size >= MAX_SIGNALS || unsupported) { skipped += name; continue }
                val t = runCatching { encode(r, flags, value, gap) }.getOrNull()
                if (t == null || t.isEmpty()) skipped += name else signals += Signal(name, frequency, t)
            }
        }
        return Result(remotes.firstNotNullOfOrNull { it.name }, signals, skipped)
    }

    private val UNSUPPORTED = setOf("RC5", "RC6", "SHIFT_ENC", "RCMM", "GRUNDIG", "BO", "SERIAL", "XMP", "GOLDSTAR")

    private fun number(s: String?): ULong? {
        val t = s?.trim() ?: return null
        return if (t.startsWith("0x") || t.startsWith("0X")) t.substring(2).toULongOrNull(16) else t.toULongOrNull()
    }

    private fun pair(r: Remote, key: String): Pair<Int, Int>? {
        val v = r.params[key] ?: return null
        val p = v.getOrNull(0)?.toIntOrNull() ?: return null
        val s = v.getOrNull(1)?.toIntOrNull() ?: 0
        return p to s
    }

    private fun single(r: Remote, key: String): Int? = r.params[key]?.firstOrNull()?.toIntOrNull()

    /** The timings for one code of a space- or pulse-encoded remote, as LIRC's transmitter builds them. */
    private fun encode(r: Remote, flags: Set<String>, value: String, gap: Int): List<Int> {
        val bits = single(r, "bits") ?: return emptyList()
        if (bits !in 1..MAX_BITS) return emptyList()
        val code = number(value) ?: return emptyList()
        val one = pair(r, "one") ?: return emptyList()
        val zero = pair(r, "zero") ?: return emptyList()
        val reverse = "REVERSE" in flags
        val out = ArrayList<Int>()
        var on = false
        fun add(isOn: Boolean, us: Int) {
            if (us <= 0) return
            if (out.isEmpty() && !isOn) return
            if (out.isNotEmpty() && isOn == on) out[out.size - 1] += us else { out += us; on = isOn }
        }
        fun data(v: ULong, n: Int) {
            for (i in 0 until n) {
                val shift = if (reverse) i else n - 1 - i
                val b = (v shr shift) and 1uL
                val (p, s) = if (b == 1uL) one else zero
                add(true, p); add(false, s)
            }
        }
        pair(r, "header")?.let { (p, s) -> add(true, p); add(false, s) }
        single(r, "plead")?.let { add(true, it) }
        val preBits = single(r, "pre_data_bits") ?: 0
        if (preBits in 1..MAX_BITS) data(number(r.params["pre_data"]?.firstOrNull()) ?: 0uL, preBits)
        pair(r, "pre")?.let { (p, s) -> add(true, p); add(false, s) }
        data(code, bits)
        pair(r, "post")?.let { (p, s) -> add(true, p); add(false, s) }
        val postBits = single(r, "post_data_bits") ?: 0
        if (postBits in 1..MAX_BITS) data(number(r.params["post_data"]?.firstOrNull()) ?: 0uL, postBits)
        single(r, "ptrail")?.let { add(true, it) }
        pair(r, "foot")?.let { (p, s) -> add(false, s); add(true, p) }
        val frameLength = out.sum()
        val trailingGap = if ("CONST_LENGTH" in flags) (gap - frameLength).coerceAtLeast(1) else gap
        add(false, trailingGap)
        val frame = out.toList()
        // min_repeat: how many times the frame is sent again after the first.
        val repeats = (single(r, "min_repeat") ?: 0).coerceIn(0, 5)
        return List(repeats + 1) { frame }.flatten()
    }
}
