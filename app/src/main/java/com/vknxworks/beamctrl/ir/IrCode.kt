package com.vknxworks.beamctrl.ir

import kotlinx.serialization.SerialName
import com.vknxworks.beamctrl.R
import androidx.annotation.StringRes
import android.content.Context
import kotlinx.serialization.Serializable

/**
 * One infrared command, stored the way people know it (protocol, address, command) rather than as
 * timings, so it can be shown, edited and exported. [encode] turns it into on/off timings.
 */
@Serializable
sealed interface IrCode {

    /** NEC. Standard: 8-bit address and command, each sent with its inverse. Extended: 16-bit address and command as given. */
    @Serializable
    @SerialName("nec")
    data class Nec(val address: Int, val command: Int, val extended: Boolean = false) : IrCode

    /** Samsung's 32-bit protocol: address sent twice, command and its inverse. */
    @Serializable
    @SerialName("samsung32")
    data class Samsung32(val address: Int, val command: Int) : IrCode

    /** Sony SIRC with 12, 15 or 20 bits: 7-bit command, then 5, 8 or 13 address bits. */
    @Serializable
    @SerialName("sirc")
    data class Sirc(val bits: Int, val address: Int, val command: Int) : IrCode

    /** Philips RC5 (commands 64 to 127 are sent as RC5X). */
    @Serializable
    @SerialName("rc5")
    data class Rc5(val address: Int, val command: Int) : IrCode

    /** Philips RC6, mode 0. */
    @Serializable
    @SerialName("rc6")
    data class Rc6(val address: Int, val command: Int) : IrCode

    /** A Pronto hex code, as printed in remote code databases. */
    @Serializable
    @SerialName("pronto")
    data class Pronto(val hex: String) : IrCode

    /** Raw timings in microseconds, alternating on and off, starting on. */
    @Serializable
    @SerialName("raw")
    data class Raw(val frequency: Int, val timings: List<Int>) : IrCode
}

/**
 * Bounds for codes that come from files or other people. Android refuses patterns longer than
 * two seconds anyway; these stop a hostile or broken file from using up memory before that.
 */
object IrLimits {
    const val MIN_FREQUENCY = 10_000
    const val MAX_FREQUENCY = 500_000
    /** On/off periods in one raw code. Real remotes use well under 300. */
    const val MAX_TIMINGS = 2_048
    /** One on or off period, in microseconds. */
    const val MAX_TIMING_US = 1_000_000
    /** Whole pattern, in microseconds (Android's own limit for one transmission). */
    const val MAX_PATTERN_US = 2_000_000L
    /** Characters in a Pronto hex code. */
    const val MAX_PRONTO_CHARS = 16_384
}

/** Timings ready for the blaster. [once] is sent for a tap; [repeat] keeps going while a button is held. */
class IrSignal(val frequency: Int, val once: IntArray, val repeat: IntArray)

/** The protocol's name, e.g. "NEC", "Sony 12-bit" or "Raw". */
fun IrCode.protocolLabel(context: Context): String = when (this) {
    is IrCode.Nec -> context.getString(if (extended) R.string.proto_nec_ext else R.string.proto_nec)
    is IrCode.Samsung32 -> context.getString(R.string.proto_samsung)
    is IrCode.Sirc -> context.getString(R.string.proto_sony_bits, bits)
    is IrCode.Rc5 -> context.getString(if (command >= 64) R.string.proto_rc5x else R.string.proto_rc5)
    is IrCode.Rc6 -> context.getString(R.string.proto_rc6)
    is IrCode.Pronto -> context.getString(R.string.proto_pronto)
    is IrCode.Raw -> context.getString(R.string.proto_raw)
}

/** A code that can't be sent or saved; [text] (from strings.xml, with [args]) says why. */
class IrCodeException(@StringRes val text: Int, vararg val args: Any) : IllegalArgumentException("invalid IR code")

/** Throws [IrCodeException] with [text] unless [ok]. */
fun irRequire(ok: Boolean, @StringRes text: Int, vararg args: Any) {
    if (!ok) throw IrCodeException(text, *args)
}

/** Why a code can't be used, in the phone's language. */
fun irErrorText(context: Context, e: Throwable): String =
    if (e is IrCodeException) context.getString(e.text, *e.args) else context.getString(R.string.code_invalid)

fun hex(value: Int, digits: Int): String = value.toString(16).uppercase().padStart(digits, '0')

/**
 * Encodes the code. [toggle] is the RC5/RC6 toggle bit, which flips on each new press so the
 * device can tell two presses from one held button.
 */
fun IrCode.encode(toggle: Boolean = false): IrSignal = when (this) {
    is IrCode.Nec -> Protocols.nec(address, command, extended)
    is IrCode.Samsung32 -> Protocols.samsung32(address, command)
    is IrCode.Sirc -> Protocols.sirc(bits, address, command)
    is IrCode.Rc5 -> Protocols.rc5(address, command, toggle)
    is IrCode.Rc6 -> Protocols.rc6(address, command, toggle)
    is IrCode.Pronto -> Pronto.decode(hex)
    is IrCode.Raw -> {
        irRequire(timings.size <= IrLimits.MAX_TIMINGS, R.string.code_too_many_timings, IrLimits.MAX_TIMINGS)
        val t = timings.filter { it > 0 }.toIntArray()
        irRequire(t.isNotEmpty(), R.string.code_no_timings)
        irRequire(t.all { it <= IrLimits.MAX_TIMING_US }, R.string.code_timing_too_long)
        irRequire(t.fold(0L) { acc, v -> acc + v } <= IrLimits.MAX_PATTERN_US, R.string.code_too_long)
        irRequire(frequency in IrLimits.MIN_FREQUENCY..IrLimits.MAX_FREQUENCY, R.string.code_frequency_range)
        IrSignal(frequency, t, t)
    }
}

/** Whether this code uses the RC5/RC6 toggle bit. */
val IrCode.usesToggle: Boolean get() = this is IrCode.Rc5 || this is IrCode.Rc6
