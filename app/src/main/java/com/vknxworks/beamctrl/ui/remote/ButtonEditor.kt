package com.vknxworks.beamctrl.ui.remote

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.RemoteButton
import com.vknxworks.beamctrl.ir.IrCode
import com.vknxworks.beamctrl.ir.Pronto
import com.vknxworks.beamctrl.ir.encode
import com.vknxworks.beamctrl.ir.hex
import com.vknxworks.beamctrl.ui.components.ChoiceSheet
import com.vknxworks.beamctrl.ui.components.PillButton

/** The kinds of code the editor can make. */
private enum class Proto(val label: String, val maxAddress: Int, val maxCommand: Int) {
    NEC("NEC", 0xFF, 0xFF),
    NEC_EXT("NEC ext", 0xFFFF, 0xFFFF),
    SAMSUNG("Samsung", 0xFF, 0xFF),
    SONY12("Sony 12", 0x1F, 0x7F),
    SONY15("Sony 15", 0xFF, 0x7F),
    SONY20("Sony 20", 0x1FFF, 0x7F),
    RC5("RC5", 0x1F, 0x7F),
    RC6("RC6", 0xFF, 0xFF),
    PRONTO("Pronto", 0, 0),
    RAW("Raw", 0, 0);

    val parsed: Boolean get() = this != PRONTO && this != RAW
}

private fun protoOf(code: IrCode): Proto = when (code) {
    is IrCode.Nec -> if (code.extended) Proto.NEC_EXT else Proto.NEC
    is IrCode.Samsung32 -> Proto.SAMSUNG
    is IrCode.Sirc -> when (code.bits) { 15 -> Proto.SONY15; 20 -> Proto.SONY20; else -> Proto.SONY12 }
    is IrCode.Rc5 -> Proto.RC5
    is IrCode.Rc6 -> Proto.RC6
    is IrCode.Pronto -> Proto.PRONTO
    is IrCode.Raw -> Proto.RAW
}

private fun addressOf(code: IrCode): Int? = when (code) {
    is IrCode.Nec -> code.address
    is IrCode.Samsung32 -> code.address
    is IrCode.Sirc -> code.address
    is IrCode.Rc5 -> code.address
    is IrCode.Rc6 -> code.address
    else -> null
}

private fun commandOf(code: IrCode): Int? = when (code) {
    is IrCode.Nec -> code.command
    is IrCode.Samsung32 -> code.command
    is IrCode.Sirc -> code.command
    is IrCode.Rc5 -> code.command
    is IrCode.Rc6 -> code.command
    else -> null
}

private fun parseHex(text: String, max: Int, what: String): Int {
    val t = text.trim().removePrefix("0x").removePrefix("0X")
    require(t.isNotEmpty()) { "Enter the $what" }
    val v = t.toIntOrNull(16) ?: throw IllegalArgumentException("The $what must be hex (0-9, A-F)")
    require(v in 0..max) { "The $what can be at most ${hex(max, 2)} for this protocol" }
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
    onDismiss: () -> Unit,
    onSave: (RemoteButton) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val blaster = LocalContext.current.container.blaster
    val initialCode = button?.code
    var label by remember { mutableStateOf(button?.label ?: presetRole?.label.orEmpty()) }
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

    fun build(): IrCode = when (proto) {
        Proto.PRONTO -> IrCode.Pronto(pronto.trim()).also { Pronto.decode(it.hex) }
        Proto.RAW -> {
            val f = frequency.trim().toIntOrNull() ?: throw IllegalArgumentException("Enter the frequency in Hz, e.g. 38000")
            val t = timings.split(Regex("[\\s,]+")).filter { it.isNotBlank() }.map {
                it.trim().removePrefix("+").removePrefix("-").toIntOrNull() ?: throw IllegalArgumentException("\"$it\" isn't a number")
            }
            IrCode.Raw(f, t).also { it.encode() }
        }
        else -> {
            val a = parseHex(address, proto.maxAddress, "address")
            val c = parseHex(command, proto.maxCommand, "command")
            when (proto) {
                Proto.NEC -> IrCode.Nec(a, c)
                Proto.NEC_EXT -> IrCode.Nec(a, c, extended = true)
                Proto.SAMSUNG -> IrCode.Samsung32(a, c)
                Proto.SONY12 -> IrCode.Sirc(12, a, c)
                Proto.SONY15 -> IrCode.Sirc(15, a, c)
                Proto.SONY20 -> IrCode.Sirc(20, a, c)
                Proto.RC5 -> IrCode.Rc5(a, c)
                else -> IrCode.Rc6(a, c)
            }
        }
    }

    fun attempt(block: (IrCode) -> Unit) {
        runCatching { build() }
            .onSuccess { error = null; block(it) }
            .onFailure { error = it.message ?: "That code isn't valid" }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
            Text(if (button == null) "Add button" else "Edit button", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(24) },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(onClick = { pickRole = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Place on remote: ${role?.label ?: "More buttons"}")
            }

            Text("Protocol", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Proto.entries.forEach { p ->
                    FilterChip(selected = proto == p, onClick = { proto = p; error = null }, label = { Text(p.label) })
                }
            }

            when (proto) {
                Proto.PRONTO -> OutlinedTextField(
                    value = pronto,
                    onValueChange = { pronto = it },
                    label = { Text("Pronto hex") },
                    placeholder = { Text("0000 006D 0022 0002 0157 00AC …") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Proto.RAW -> {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it.filter(Char::isDigit).take(6) },
                        label = { Text("Carrier frequency (Hz)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = timings,
                        onValueChange = { timings = it },
                        label = { Text("Timings in µs, on and off in turn") },
                        placeholder = { Text("9000 4500 560 560 560 1690 …") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it.take(6) },
                        label = { Text("Address (hex)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it.take(6) },
                        label = { Text("Command (hex)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { attempt { blaster.send(it) } }) { Text("Test") }
                PillButton(
                    "Save",
                    onClick = {
                        attempt { code ->
                            val name = label.trim().ifEmpty { role?.label ?: "Button" }
                            onSave(
                                (button ?: RemoteButton(label = name, code = code)).copy(label = name, role = role, code = code),
                            )
                        }
                    },
                    icon = Icons.Filled.SettingsRemote,
                    modifier = Modifier.weight(1f),
                )
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    androidx.compose.material3.Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Text("  Delete button", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (pickRole) {
        val options: List<Pair<KeyRole?, String>> = listOf<Pair<KeyRole?, String>>(null to "More buttons") + KeyRole.entries.map { it to it.label }
        ChoiceSheet("Place on remote", options, role, onDismiss = { pickRole = false }) { picked ->
            // Follow the spot's name unless the button has been given its own.
            if (label.isBlank() || label == role?.label) label = picked?.label ?: label
            role = picked
            pickRole = false
        }
    }
}
