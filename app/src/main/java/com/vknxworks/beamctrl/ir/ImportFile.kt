package com.vknxworks.beamctrl.ir

import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.RemoteButton

/**
 * "Import a file" on the Add tab: works out what kind of file it is (irplus, LIRC or Flipper Zero `.ir`) and
 * turns its signals into buttons. Only codes the blaster can send are kept; the rest are reported as skipped.
 */
object ImportFile {

    fun read(text: String, fallbackLabel: String): FlipperIr.ImportResult = when {
        IrplusFile.looksLike(text) -> fromIrplus(IrplusFile.parse(text), fallbackLabel)
        LircFile.looksLike(text) -> fromLirc(LircFile.parse(text), fallbackLabel)
        else -> FlipperIr.parse(text, fallbackLabel)
    }

    private class Builder(private val fallbackLabel: String) {
        val buttons = ArrayList<RemoteButton>()
        val skipped = ArrayList<String>()
        private val usedRoles = HashSet<KeyRole>()

        fun add(name: String, code: IrCode) {
            val label = FlipperIr.prettyName(name).ifEmpty { fallbackLabel }
            // Only codes the blaster can actually send (frequency, length and timing limits).
            if (runCatching { code.encode() }.isFailure) {
                skipped += label
                return
            }
            // Only the first button for each role goes on the layout; the rest stay in "More buttons".
            val role = FlipperIr.roleFor(name)?.takeIf { usedRoles.add(it) }
            buttons += RemoteButton(label = label, role = role, code = code)
        }
    }

    private fun fromLirc(r: LircFile.Result, fallbackLabel: String): FlipperIr.ImportResult {
        val b = Builder(fallbackLabel)
        r.signals.forEach { s -> b.add(s.name, IrCode.Raw(s.frequency, s.timings)) }
        b.skipped += r.skipped.map { FlipperIr.prettyName(it) }
        return FlipperIr.ImportResult(b.buttons, b.skipped, name = r.remoteName?.replace('_', ' ')?.trim()?.take(80))
    }

    private fun fromIrplus(r: IrplusFile.Result, fallbackLabel: String): FlipperIr.ImportResult {
        val b = Builder(fallbackLabel)
        r.buttons.forEach { button ->
            val code = when (val c = button.code) {
                is IrplusFile.Code.Pronto -> IrCode.Pronto(c.hex)
                is IrplusFile.Code.Raw -> IrCode.Raw(c.frequency, c.timings)
                is IrplusFile.Code.Nec ->
                    if (c.standard) IrCode.Nec(c.address and 0xFF, c.command and 0xFF)
                    else IrCode.Nec(c.address, c.command, extended = true)
            }
            b.add(button.label, code)
        }
        b.skipped += r.skipped.map { FlipperIr.prettyName(it).ifEmpty { fallbackLabel } }
        return FlipperIr.ImportResult(b.buttons, b.skipped, name = r.model?.take(80), brand = r.manufacturer?.take(80))
    }
}
