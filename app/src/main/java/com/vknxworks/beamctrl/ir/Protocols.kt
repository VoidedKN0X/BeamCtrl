package com.vknxworks.beamctrl.ir

import kotlin.math.roundToInt
import com.vknxworks.beamctrl.R

/** Builds an on/off timing list, merging consecutive periods of the same state. Always starts with on. */
internal class Pulses {
    private val out = ArrayList<Int>()
    private var lastOn = false

    val total: Int get() = out.sum()

    fun on(us: Int) = add(true, us)
    fun off(us: Int) = add(false, us)

    private fun add(isOn: Boolean, us: Int) {
        if (us <= 0) return
        if (out.isEmpty()) {
            // The LED is idle (off) before the signal, so a leading off period is just silence.
            if (!isOn) return
            out.add(us)
            lastOn = true
            return
        }
        if (isOn == lastOn) out[out.size - 1] = out.last() + us
        else {
            out.add(us)
            lastOn = isOn
        }
    }

    /** Adds silence so the frame lasts [periodUs] from its start, giving the receiver its gap. */
    fun padTo(periodUs: Int, minGapUs: Int = 0) {
        val gap = (periodUs - total).coerceAtLeast(minGapUs)
        off(gap)
    }

    fun build(): IntArray = out.toIntArray()
}

internal fun concat(vararg frames: IntArray): IntArray {
    // Every frame ends with off and starts with on, so frames can simply be joined.
    val result = IntArray(frames.sumOf { it.size })
    var i = 0
    for (f in frames) {
        f.copyInto(result, i)
        i += f.size
    }
    return result
}

object Protocols {

    // ---------- NEC ----------

    private const val NEC_UNIT = 562

    fun nec(address: Int, command: Int, extended: Boolean): IrSignal {
        val ext = extended || address > 0xFF || command > 0xFF
        val bits: Long = if (ext) {
            (address.toLong() and 0xFFFF) or ((command.toLong() and 0xFFFF) shl 16)
        } else {
            val a = address.toLong() and 0xFF
            val c = command.toLong() and 0xFF
            a or ((a.inv() and 0xFF) shl 8) or (c shl 16) or ((c.inv() and 0xFF) shl 24)
        }
        val frame = Pulses().apply {
            on(9000); off(4500)
            for (i in 0 until 32) {
                on(NEC_UNIT)
                off(if ((bits shr i) and 1L == 1L) NEC_UNIT * 3 else NEC_UNIT)
            }
            on(NEC_UNIT)
            padTo(108_000)
        }.build()
        // While held, NEC sends a short "repeat" frame instead of the whole command.
        val repeat = Pulses().apply {
            on(9000); off(2250); on(NEC_UNIT)
            padTo(108_000)
        }.build()
        return IrSignal(38_000, frame, repeat)
    }

    // ---------- Samsung32 ----------

    fun samsung32(address: Int, command: Int): IrSignal {
        val a = address.toLong() and 0xFF
        val c = command.toLong() and 0xFF
        val bits = a or (a shl 8) or (c shl 16) or ((c.inv() and 0xFF) shl 24)
        val frame = Pulses().apply {
            on(4500); off(4500)
            for (i in 0 until 32) {
                on(560)
                off(if ((bits shr i) and 1L == 1L) 1690 else 560)
            }
            on(560)
            padTo(108_000)
        }.build()
        return IrSignal(38_000, frame, frame)
    }

    // ---------- Sony SIRC ----------

    fun sirc(bits: Int, address: Int, command: Int): IrSignal {
        irRequire(bits == 12 || bits == 15 || bits == 20, R.string.code_sony_bits)
        val addressBits = bits - 7
        val value: Long = (command.toLong() and 0x7F) or ((address.toLong() and ((1L shl addressBits) - 1)) shl 7)
        val frame = Pulses().apply {
            on(2400); off(600)
            for (i in 0 until bits) {
                on(if ((value shr i) and 1L == 1L) 1200 else 600)
                off(600)
            }
            padTo(45_000)
        }.build()
        // Sony devices only act after seeing the frame at least three times.
        return IrSignal(40_000, concat(frame, frame, frame), frame)
    }

    // ---------- Philips RC5 ----------

    private const val RC5_HALF = 889

    fun rc5(address: Int, command: Int, toggle: Boolean): IrSignal {
        val c = command and 0x7F
        val bits = buildList {
            add(true) // start bit
            add(c < 64) // second start bit; inverted 7th command bit (RC5X)
            add(toggle)
            for (i in 4 downTo 0) add((address shr i) and 1 == 1)
            for (i in 5 downTo 0) add((c shr i) and 1 == 1)
        }
        val frame = Pulses().apply {
            // RC5 Manchester: a 1 is off then on, a 0 is on then off.
            for (b in bits) {
                if (b) { off(RC5_HALF); on(RC5_HALF) } else { on(RC5_HALF); off(RC5_HALF) }
            }
            padTo(113_778 - RC5_HALF)
        }.build()
        return IrSignal(36_000, frame, frame)
    }

    // ---------- Philips RC6 (mode 0) ----------

    private const val RC6_T = 444

    fun rc6(address: Int, command: Int, toggle: Boolean): IrSignal {
        val frame = Pulses().apply {
            on(RC6_T * 6); off(RC6_T * 2) // leader
            // RC6 Manchester is the other way round to RC5: a 1 is on then off.
            fun bit(b: Boolean, width: Int = RC6_T) {
                if (b) { on(width); off(width) } else { off(width); on(width) }
            }
            bit(true) // start bit
            bit(false); bit(false); bit(false) // mode 0
            bit(toggle, RC6_T * 2) // trailer bit is double width
            for (i in 7 downTo 0) bit((address shr i) and 1 == 1)
            for (i in 7 downTo 0) bit((command shr i) and 1 == 1)
            padTo(107_000, minGapUs = RC6_T * 6)
        }.build()
        return IrSignal(36_000, frame, frame)
    }
}

object Pronto {

    /** Decodes "0000 006D 0022 0002 0157 00AC …" (learned, modulated codes). */
    fun decode(hex: String): IrSignal {
        irRequire(hex.length <= IrLimits.MAX_PRONTO_CHARS, R.string.pronto_too_long)
        val words = hex.trim().split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.map {
            // Pronto words are four hex digits; anything bigger is not a Pronto code (and would overflow below).
            it.toIntOrNull(16)?.takeIf { v -> v in 0..0xFFFF } ?: throw IrCodeException(R.string.pronto_not_hex, it.take(12))
        }
        irRequire(words.size >= 6, R.string.pronto_too_short)
        irRequire(words[0] == 0x0000, R.string.pronto_not_learned)
        val freqWord = words[1]
        irRequire(freqWord > 0, R.string.pronto_frequency_zero)
        val frequency = (1_000_000.0 / (freqWord * 0.241246)).roundToInt()
        irRequire(frequency in IrLimits.MIN_FREQUENCY..IrLimits.MAX_FREQUENCY, R.string.pronto_frequency_range)
        val n1 = words[2] * 2
        val n2 = words[3] * 2
        irRequire(n1 + n2 > 0, R.string.pronto_no_timings)
        irRequire(words.size >= 4 + n1 + n2, R.string.pronto_short_header)
        val periodUs = 1_000_000.0 / frequency
        fun seq(from: Int, count: Int) = IntArray(count) { i -> (words[from + i] * periodUs).roundToInt().coerceAtLeast(1) }
        val first = seq(4, n1)
        val second = seq(4 + n1, n2)
        val once = if (first.isNotEmpty()) first else second
        val repeat = if (second.isNotEmpty()) second else first
        for (part in listOf(once, repeat)) {
            irRequire(part.fold(0L) { acc, v -> acc + v } <= IrLimits.MAX_PATTERN_US, R.string.pronto_too_long_time)
        }
        return IrSignal(frequency, once, repeat)
    }

    /** Encodes timings as a Pronto code with everything in the one-time part. */
    fun encode(frequency: Int, timings: IntArray): String {
        val freqWord = (1_000_000.0 / (frequency * 0.241246)).roundToInt()
        val periodUs = 1_000_000.0 / frequency
        val padded = if (timings.size % 2 == 0) timings else timings + intArrayOf(40_000)
        val words = buildList {
            add(0); add(freqWord); add(padded.size / 2); add(0)
            padded.forEach { add((it / periodUs).roundToInt().coerceIn(1, 0xFFFF)) }
        }
        return words.joinToString(" ") { hex(it, 4) }
    }
}
