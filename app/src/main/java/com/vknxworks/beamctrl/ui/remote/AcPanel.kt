package com.vknxworks.beamctrl.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vknxworks.beamctrl.R
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.AcControl
import com.vknxworks.beamctrl.data.AcMode
import com.vknxworks.beamctrl.data.AcState
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.data.RemoteButton
import com.vknxworks.beamctrl.ui.components.CtrlCard
import com.vknxworks.beamctrl.ui.components.CtrlSquareButton
import com.vknxworks.beamctrl.ui.components.SectionLabel
import com.vknxworks.beamctrl.ui.theme.CtrlColors
import com.vknxworks.beamctrl.ui.theme.CtrlShapes
import com.vknxworks.beamctrl.ui.theme.CtrlType
import com.vknxworks.beamctrl.ui.theme.Gutter
import java.text.DateFormat
import java.util.Date

/** "Cool · 22 °C · Fan high", in the phone's language. */
@Composable
fun acStateText(s: AcState): String {
    val parts = buildList {
        add(stringResource(s.mode.label))
        s.temperature?.let { add(stringResource(R.string.ac_degrees, it)) }
        s.fan?.let { add(stringResource(R.string.ac_fan_value, stringResource(it.label))) }
    }
    return parts.joinToString(" · ")
}

/**
 * Air-conditioner remotes: the state BeamCtrl last sent, and controls that pick the remote's own button for the
 * next state (one degree up or down, another mode or fan speed). Only buttons with a state (set in the button
 * editor, or recognised from names such as "Cool_hi" or "Heat 22") take part.
 */
@Composable
fun AcPanel(remote: Remote, onSend: (RemoteButton) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val states = AcControl.states(remote)
    CtrlCard(modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.ac_now))
        val cur = remote.acState
        if (cur == null) {
            Text(stringResource(R.string.ac_unknown), style = MaterialTheme.typography.bodyMedium, color = CtrlColors.Muted, modifier = Modifier.padding(top = 6.dp))
        } else {
            Text(acStateText(cur), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
            if (remote.acStateAt > 0) {
                Text(
                    stringResource(R.string.ac_sent_at, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(remote.acStateAt))),
                    style = CtrlType.Data,
                    color = CtrlColors.Muted,
                )
            }
        }
        if (states.isEmpty()) {
            Text(stringResource(R.string.ac_no_states), style = MaterialTheme.typography.bodyMedium, color = CtrlColors.Muted, modifier = Modifier.padding(top = 10.dp))
            return@CtrlCard
        }
        if (cur?.temperature != null) {
            val down = AcControl.step(remote, cur, -1)
            val up = AcControl.step(remote, cur, +1)
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CtrlSquareButton(Icons.Outlined.Remove, stringResource(R.string.ac_colder), onClick = { down?.let(onSend) }, enabled = down != null)
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.ac_degrees, cur.temperature), style = CtrlType.Key)
                Spacer(Modifier.width(16.dp))
                CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.ac_warmer), onClick = { up?.let(onSend) }, enabled = up != null)
            }
        }
        val modes = AcControl.modes(remote)
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { mode ->
                    val b = AcControl.forMode(remote, cur, mode)
                    AcChip(context.getString(mode.label), selected = cur?.mode == mode, enabled = b != null) { b?.let(onSend) }
                }
            }
            if (cur != null && cur.mode != AcMode.OFF) {
                val fans = AcControl.fans(remote, cur.mode)
                if (fans.size > 1) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fans.forEach { fan ->
                            val b = AcControl.forFan(remote, cur, fan)
                            AcChip(context.getString(R.string.ac_fan_value, context.getString(fan.label)), selected = cur.fan == fan, enabled = b != null) { b?.let(onSend) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(text, style = CtrlType.Data) },
        shape = CtrlShapes.Chip,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CtrlColors.Surface,
            labelColor = CtrlColors.Text,
            selectedContainerColor = CtrlColors.Accent,
            selectedLabelColor = CtrlColors.OnAccent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = CtrlColors.ControlOutline,
            selectedBorderColor = CtrlColors.Accent,
        ),
    )
}

/** Padding the panel gets on the remote screen. */
val AcPanelPadding = Modifier.padding(horizontal = Gutter, vertical = 8.dp)
