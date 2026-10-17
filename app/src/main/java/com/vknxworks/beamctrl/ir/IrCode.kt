package com.vknxworks.beamctrl.ir

import kotlinx.serialization.SerialName
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

/** Timings ready for the blaster. [once] is sent for a tap; [repeat] keeps going while a button is held. */
class IrSignal(val frequency: Int, val once: IntArray, val repeat: IntArray)

val IrCode.protocolLabel: String
    get() = when (this) {
        is IrCode.Nec -> if (extended) "NEC extended" else "NEC"
        is IrCode.Samsung32 -> "Samsung"
        is IrCode.Sirc -> "Sony $bits-bit"
        is IrCode.Rc5 -> if (command >= 64) "RC5X" else "RC5"
        is IrCode.Rc6 -> "RC6"
        is IrCode.Pronto -> "Pronto"
        is IrCode.Raw -> "Raw"
    }

/** Short description of the code for lists and the editor, e.g. "NEC  04 · 08". */
val IrCode.summary: String
    get() = when (this) {
        is IrCode.Nec -> "$protocolLabel  ${hex(address, if (extended) 4 else 2)} · ${hex(command, if (extended) 4 else 2)}"
        is IrCode.Samsung32 -> "$protocolLabel  ${hex(address, 2)} · ${hex(command, 2)}"
        is IrCode.Sirc -> "$protocolLabel  ${hex(address, 2)} · ${hex(command, 2)}"
        is IrCode.Rc5 -> "$protocolLabel  ${hex(address, 2)} · ${hex(command, 2)}"
        is IrCode.Rc6 -> "$protocolLabel  ${hex(address, 2)} · ${hex(command, 2)}"
        is IrCode.Pronto -> "$protocolLabel  ${hex.trim().split(Regex("\\s+")).size} words"
        is IrCode.Raw -> "$protocolLabel  ${timings.size} timings at ${frequency / 1000} kHz"
    }

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
        val t = timings.filter { it > 0 }.toIntArray()
        require(t.isNotEmpty()) { "No timings" }
        require(frequency in 10_000..500_000) { "Frequency out of range" }
        IrSignal(frequency, t, t)
    }
}

/** Whether this code uses the RC5/RC6 toggle bit. */
val IrCode.usesToggle: Boolean get() = this is IrCode.Rc5 || this is IrCode.Rc6
