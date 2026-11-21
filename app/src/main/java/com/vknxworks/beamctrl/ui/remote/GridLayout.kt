package com.vknxworks.beamctrl.ui.remote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.beamctrl.R
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.ui.theme.CtrlColors
import com.vknxworks.beamctrl.ui.theme.CtrlShapes
import com.vknxworks.beamctrl.ui.theme.Gutter

/**
 * The "Buttons in a grid" layout: every button of the remote, in your own order, [Remote.gridColumns] per row.
 * The order is changed in the button editor (Move earlier / Move later).
 */
@Composable
fun GridRemoteLayout(remote: Remote, editing: Boolean, onAddButton: () -> Unit) {
    val columns = remote.gridColumns.coerceIn(2, 4)
    val cells: List<Any> = remote.buttons + if (editing) listOf(AddCell) else emptyList()
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Gutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        cells.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { cell ->
                    if (cell === AddCell) {
                        Surface(
                            onClick = onAddButton,
                            shape = CtrlShapes.Field,
                            color = Color.Transparent,
                            contentColor = CtrlColors.AccentText,
                            border = BorderStroke(1.dp, CtrlColors.AccentText),
                            modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.add_button), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else {
                        val button = cell as com.vknxworks.beamctrl.data.RemoteButton
                        val repeat = button.role in REPEAT_ROLES || button.role == null
                        BeamKey(
                            button, button.role,
                            Modifier.weight(1f).heightIn(min = 60.dp),
                            shape = CtrlShapes.Field,
                            container = if (button.role == KeyRole.POWER) CtrlColors.Accent else CtrlColors.Surface,
                            contentColor = if (button.role == KeyRole.POWER) CtrlColors.OnAccent else CtrlColors.Text,
                            borderColor = if (button.role == KeyRole.POWER) null else CtrlColors.Outline,
                            repeat = repeat,
                        ) {
                            Text(
                                button.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
                // Keep the last row's buttons the same width as the others.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private object AddCell

private val REPEAT_ROLES = setOf(
    KeyRole.VOL_UP, KeyRole.VOL_DOWN, KeyRole.CH_UP, KeyRole.CH_DOWN,
    KeyRole.UP, KeyRole.DOWN, KeyRole.LEFT, KeyRole.RIGHT, KeyRole.REWIND, KeyRole.FAST_FORWARD,
)
