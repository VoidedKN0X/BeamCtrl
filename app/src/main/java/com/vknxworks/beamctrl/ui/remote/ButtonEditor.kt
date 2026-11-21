package com.vknxworks.beamctrl.ui.remote

import com.vknxworks.beamctrl.ui.components.CtrlBottomSheet
import com.vknxworks.beamctrl.ui.components.SectionLabel
import com.vknxworks.beamctrl.ui.theme.CtrlType
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.layout.height
import com.vknxworks.beamctrl.ui.components.ctrlFieldColors
import com.vknxworks.beamctrl.ui.components.CtrlSecondaryButton
import com.vknxworks.beamctrl.ui.theme.CtrlShapes
import com.vknxworks.beamctrl.ui.theme.CtrlColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import com.vknxworks.beamctrl.ir.irRequire
import com.vknxworks.beamctrl.ir.irErrorText
import com.vknxworks.beamctrl.ir.IrCodeException
import androidx.annotation.StringRes
import com.vknxworks.beamctrl.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.AcFan
import com.vknxworks.beamctrl.data.AcMode
import com.vknxworks.beamctrl.data.AcState
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.RemoteButton
import com.vknxworks.beamctrl.ir.IrCode
import com.vknxworks.beamctrl.ir.Pronto
import com.vknxworks.beamctrl.ir.encode
import com.vknxworks.beamctrl.ir.hex
import com.vknxworks.beamctrl.ui.components.ChoiceSheet
import com.vknxworks.beamctrl.ui.components.PillButton

/** The kinds of code the editor can make. */
private enum class Proto(@StringRes val label: Int, val maxAddress: Int, val maxCommand: Int) {
    NEC(R.string.proto_nec, 0xFF, 0xFF),
    NEC_EXT(R.string.chip_nec_ext, 0xFFFF, 0xFFFF),
    SAMSUNG(R.string.proto_samsung, 0xFF, 0xFF),
    SONY12(R.string.chip_sony_12, 0x1F, 0x7F),
    SONY15(R.string.chip_sony_15, 0xFF, 0x7F),
    SONY20(R.string.chip_sony_20, 0x1FFF, 0x7F),
    RC5(R.string.proto_rc5, 0x1F, 0x7F),
    RC6(R.string.proto_rc6, 0xFF, 0xFF),
    PANASONIC(R.string.proto_panasonic, 0xFFF, 0xFF),
    JVC(R.string.proto_jvc, 0xFF, 0xFF),
    DENON(R.string.proto_denon, 0x1F, 0xFF),
    SHARP(R.string.proto_sharp, 0x1F, 0xFF),
    PRONTO(R.string.proto_pronto, 0, 0),
    RAW(R.string.proto_raw, 0, 0);

    val parsed: Boolean get() = this != PRONTO && this != RAW
}

private fun protoOf(code: IrCode): Proto = when (code) {
    is IrCode.Nec -> if (code.extended) Proto.NEC_EXT else Proto.NEC
    is IrCode.Samsung32 -> Proto.SAMSUNG
    is IrCode.Sirc -> when (code.bits) { 15 -> Proto.SONY15; 20 -> Proto.SONY20; else -> Proto.SONY12 }
    is IrCode.Rc5 -> Proto.RC5
    is IrCode.Rc6 -> Proto.RC6
    is IrCode.Panasonic -> Proto.PANASONIC
    is IrCode.Jvc -> Proto.JVC
    is IrCode.Denon -> Proto.DENON
    is IrCode.Sharp -> Proto.SHARP
    is IrCode.Pronto -> Proto.PRONTO
    is IrCode.Raw -> Proto.RAW
}

private fun addressOf(code: IrCode): Int? = when (code) {
    is IrCode.Nec -> code.address
    is IrCode.Samsung32 -> code.address
    is IrCode.Sirc -> code.address
    is IrCode.Rc5 -> code.address
    is IrCode.Rc6 -> code.address
    is IrCode.Panasonic -> code.address
    is IrCode.Jvc -> code.address
    is IrCode.Denon -> code.address
    is IrCode.Sharp -> code.address
    else -> null
}

private fun commandOf(code: IrCode): Int? = when (code) {
    is IrCode.Nec -> code.command
    is IrCode.Samsung32 -> code.command
    is IrCode.Sirc -> code.command
    is IrCode.Rc5 -> code.command
    is IrCode.Rc6 -> code.command
    is IrCode.Panasonic -> code.command
    is IrCode.Jvc -> code.command
    is IrCode.Denon -> code.command
    is IrCode.Sharp -> code.command
    else -> null
}

/** Reads a hex address ([isAddress]) or command; a mistake throws [IrCodeException] with the reason. */
private fun parseHex(text: String, max: Int, isAddress: Boolean): Int {
    val t = text.trim().removePrefix("0x").removePrefix("0X")
    irRequire(t.isNotEmpty(), if (isAddress) R.string.edit_enter_address else R.string.edit_enter_command)
    val v = t.toIntOrNull(16) ?: throw IrCodeException(if (isAddress) R.string.edit_address_not_hex else R.string.edit_command_not_hex)
    irRequire(v in 0..max, if (isAddress) R.string.edit_address_max else R.string.edit_command_max, hex(max, 2))
    return v
}

/**
 * Sheet for adding or changing one button: its name, where it sits on the remote, and its code.
 * [button] is null when adding; [presetRole] fills the spot that was tapped.
 */
@Composable
fun ButtonEditorSheet(
    button: RemoteButton?,
    presetRole: KeyRole?,
    /** Air-conditioner remote: offer "This button sets" (mode, temperature, fan). */
    isAc: Boolean = false,
    /** Grid layout: move the button one place earlier or later (null when it can't move that way). */
    onMoveEarlier: (() -> Unit)? = null,
    onMoveLater: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (RemoteButton) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    val blaster = context.container.blaster
    val initialCode = button?.code
    var label by remember { mutableStateOf(button?.label ?: presetRole?.label(context).orEmpty()) }
    var role by remember { mutableStateOf(button?.role ?: presetRole) }
    var proto by remember { mutableStateOf(initialCode?.let(::protoOf) ?: Proto.NEC) }
    var address by remember { mutableStateOf(initialCode?.let(::addressOf)?.let { hex(it, 2) }.orEmpty()) }
    var command by remember { mutableStateOf(initialCode?.let(::commandOf)?.let { hex(it, 2) }.orEmpty()) }
    var pronto by remember {
        mutableStateOf(
            when (initialCode) {
                is IrCode.Pronto -> initialCode.hex
                else -> ""
            },
        )
    }
    var frequency by remember { mutableStateOf(if (initialCode is IrCode.Raw) initialCode.frequency.toString() else "38000") }
    var timings by remember { mutableStateOf(if (initialCode is IrCode.Raw) initialCode.timings.joinToString(" ") else "") }
    var error by remember { mutableStateOf<String?>(null) }
    var pickRole by remember { mutableStateOf(false) }
    // Air conditioners: what this button sets. Starts from what was saved, or a guess from the name.
    val initialAc = button?.ac ?: button?.let { AcState.guess(it.label) }
    var acMode by remember { mutableStateOf(initialAc?.mode) }
    var acTemp by remember { mutableStateOf(initialAc?.temperature?.toString().orEmpty()) }
    var acFan by remember { mutableStateOf(initialAc?.fan) }

    fun build(): IrCode = when (proto) {
        Proto.PRONTO -> IrCode.Pronto(pronto.trim()).also { Pronto.decode(it.hex) }
        Proto.RAW -> {
            val f = frequency.trim().toIntOrNull() ?: throw IrCodeException(R.string.edit_enter_frequency)
            val t = timings.split(Regex("[\\s,]+")).filter { it.isNotBlank() }.map {
                it.trim().removePrefix("+").removePrefix("-").toIntOrNull() ?: throw IrCodeException(R.string.edit_not_number, it.take(12))
            }
            IrCode.Raw(f, t).also { it.encode() }
        }
        else -> {
            val a = parseHex(address, proto.maxAddress, isAddress = true)
            val c = parseHex(command, proto.maxCommand, isAddress = false)
            when (proto) {
                Proto.NEC -> IrCode.Nec(a, c)
                Proto.NEC_EXT -> IrCode.Nec(a, c, extended = true)
                Proto.SAMSUNG -> IrCode.Samsung32(a, c)
                Proto.SONY12 -> IrCode.Sirc(12, a, c)
                Proto.SONY15 -> IrCode.Sirc(15, a, c)
                Proto.SONY20 -> IrCode.Sirc(20, a, c)
                Proto.RC5 -> IrCode.Rc5(a, c)
                Proto.PANASONIC -> IrCode.Panasonic(a, c)
                Proto.JVC -> IrCode.Jvc(a, c)
                Proto.DENON -> IrCode.Denon(a, c)
                Proto.SHARP -> IrCode.Sharp(a, c)
                else -> IrCode.Rc6(a, c)
            }
        }
    }

    fun attempt(block: (IrCode) -> Unit) {
        runCatching { build() }
            .onSuccess { error = null; block(it) }
            .onFailure { error = irErrorText(context, it) }
    }

    CtrlBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(if (button == null) R.string.add_button else R.string.edit_button), style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                shape = CtrlShapes.Field,
                colors = ctrlFieldColors(),
                value = label,
                onValueChange = { label = it.take(24) },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            CtrlSecondaryButton(onClick = { pickRole = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.place_on_remote_value, role?.label(context) ?: stringResource(R.string.more_buttons)))
            }

            SectionLabel(stringResource(R.string.protocol))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Proto.entries.forEach { p ->
                    FilterChip(
                        selected = proto == p,
                        onClick = { proto = p; error = null },
                        label = { Text(stringResource(p.label), style = CtrlType.Data) },
                        shape = CtrlShapes.Chip,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CtrlColors.Surface,
                            labelColor = CtrlColors.Text,
                            selectedContainerColor = CtrlColors.Accent,
                            selectedLabelColor = CtrlColors.OnAccent,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = proto == p,
                            borderColor = CtrlColors.ControlOutline,
                            selectedBorderColor = CtrlColors.Accent,
                        ),
                    )
                }
            }

            when (proto) {
                Proto.PRONTO -> OutlinedTextField(
                    shape = CtrlShapes.Field,
                    colors = ctrlFieldColors(),
                    value = pronto,
                    onValueChange = { pronto = it },
                    label = { Text(stringResource(R.string.pronto_hex)) },
                    placeholder = { Text("0000 006D 0022 0002 0157 00AC …") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Proto.RAW -> {
                    OutlinedTextField(
                        shape = CtrlShapes.Field,
                        colors = ctrlFieldColors(),
                        value = frequency,
                        onValueChange = { frequency = it.filter(Char::isDigit).take(6) },
                        label = { Text(stringResource(R.string.carrier_frequency)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        shape = CtrlShapes.Field,
                        colors = ctrlFieldColors(),
                        value = timings,
                        onValueChange = { timings = it },
                        label = { Text(stringResource(R.string.timings_label)) },
                        placeholder = { Text("9000 4500 560 560 560 1690 …") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        shape = CtrlShapes.Field,
                        colors = ctrlFieldColors(),
                        value = address,
                        onValueChange = { address = it.take(6) },
                        label = { Text(stringResource(R.string.address_hex)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        shape = CtrlShapes.Field,
                        colors = ctrlFieldColors(),
                        value = command,
                        onValueChange = { command = it.take(6) },
                        label = { Text(stringResource(R.string.command_hex)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (isAc) {
                SectionLabel(stringResource(R.string.ac_button_sets))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorChip(stringResource(R.string.ac_nothing), acMode == null) { acMode = null }
                    AcMode.entries.forEach { m -> EditorChip(stringResource(m.label), acMode == m) { acMode = m } }
                }
                if (acMode != null && acMode != AcMode.OFF) {
                    OutlinedTextField(
                        shape = CtrlShapes.Field,
                        colors = ctrlFieldColors(),
                        value = acTemp,
                        onValueChange = { acTemp = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.ac_temperature_field, AcState.MIN_TEMP, AcState.MAX_TEMP)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditorChip(stringResource(R.string.ac_fan_unknown), acFan == null) { acFan = null }
                        AcFan.entries.forEach { f -> EditorChip(stringResource(f.label), acFan == f) { acFan = f } }
                    }
                }
                Text(stringResource(R.string.ac_button_sets_hint), style = MaterialTheme.typography.bodySmall, color = CtrlColors.Muted)
            }

            if (onMoveEarlier != null || onMoveLater != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CtrlSecondaryButton(onClick = { onMoveEarlier?.invoke() }, enabled = onMoveEarlier != null, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.move_earlier)) }
                    CtrlSecondaryButton(onClick = { onMoveLater?.invoke() }, enabled = onMoveLater != null, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.move_later)) }
                }
            }

            error?.let { Text(it, color = CtrlColors.Error, style = MaterialTheme.typography.bodyMedium) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CtrlSecondaryButton(onClick = { attempt { blaster.send(it) } }, modifier = Modifier.height(56.dp)) { Text(stringResource(R.string.test)) }
                PillButton(
                    stringResource(R.string.save),
                    onClick = {
                        attempt { code ->
                            val name = label.trim().ifEmpty { role?.label(context) ?: context.getString(R.string.button_default) }
                            val acResult = runCatching {
                                if (!isAc) button?.ac else acMode?.let { m ->
                                    if (m == AcMode.OFF) AcState(AcMode.OFF) else {
                                        val t = acTemp.trim().takeIf { it.isNotEmpty() }?.let { txt ->
                                            txt.toIntOrNull()?.takeIf { it in AcState.MIN_TEMP..AcState.MAX_TEMP }
                                                ?: throw IrCodeException(R.string.ac_temperature_range, AcState.MIN_TEMP, AcState.MAX_TEMP)
                                        }
                                        AcState(m, t, acFan)
                                    }
                                }
                            }
                            val failure = acResult.exceptionOrNull()
                            if (failure != null) {
                                error = irErrorText(context, failure)
                                return@attempt
                            }
                            val ac = acResult.getOrNull()
                            onSave(
                                (button ?: RemoteButton(label = name, code = code)).copy(label = name, role = role, code = code, ac = ac),
                            )
                        }
                    },
                    icon = Icons.Outlined.SettingsRemote,
                    modifier = Modifier.weight(1f),
                )
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    androidx.compose.material3.Icon(Icons.Outlined.Delete, null, tint = CtrlColors.Error)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_button), color = CtrlColors.Error)
                }
            }
        }
    }

    if (pickRole) {
        val options: List<Pair<KeyRole?, String>> =
            listOf<Pair<KeyRole?, String>>(null to stringResource(R.string.more_buttons)) + KeyRole.entries.map { it to it.label(context) }
        ChoiceSheet(stringResource(R.string.place_on_remote), options, role, onDismiss = { pickRole = false }) { picked ->
            // Follow the spot's name unless the button has been given its own.
            if (label.isBlank() || label == role?.label(context)) label = picked?.label(context) ?: label
            role = picked
            pickRole = false
        }
    }
}

@Composable
private fun EditorChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = CtrlType.Data) },
        shape = CtrlShapes.Chip,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CtrlColors.Surface,
            labelColor = CtrlColors.Text,
            selectedContainerColor = CtrlColors.Accent,
            selectedLabelColor = CtrlColors.OnAccent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = CtrlColors.ControlOutline,
            selectedBorderColor = CtrlColors.Accent,
        ),
    )
}
