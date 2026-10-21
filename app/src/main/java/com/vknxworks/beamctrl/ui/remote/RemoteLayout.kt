package com.vknxworks.beamctrl.ui.remote

import androidx.compose.foundation.layout.Arrangement
import com.vknxworks.beamctrl.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.data.RemoteButton
import com.vknxworks.beamctrl.ui.theme.Gutter

private val dpadRoles = listOf(KeyRole.UP, KeyRole.DOWN, KeyRole.LEFT, KeyRole.RIGHT, KeyRole.OK)
private val navRoles = listOf(KeyRole.BACK, KeyRole.HOME, KeyRole.MENU)
private val rockerRoles = listOf(KeyRole.VOL_UP, KeyRole.VOL_DOWN, KeyRole.CH_UP, KeyRole.CH_DOWN, KeyRole.INFO, KeyRole.EXIT, KeyRole.GUIDE)
private val mediaRoles = listOf(KeyRole.REWIND, KeyRole.PLAY, KeyRole.PLAY_PAUSE, KeyRole.PAUSE, KeyRole.STOP, KeyRole.FAST_FORWARD)
private val digitRoles = (0..9).map { KeyRole.digit(it) }

/** Buttons that don't have a place on the layout: no role, or a second button for a role. */
fun extraButtons(remote: Remote): List<RemoteButton> {
    val seen = mutableSetOf<KeyRole>()
    return remote.buttons.filter { b -> b.role == null || !seen.add(b.role) }
}

/**
 * The remote itself: power row, d-pad, volume and channel rockers, playback, numbers and any
 * other buttons. Sections only appear when the remote has buttons for them (all of them show in
 * edit mode, so empty spots can be filled in).
 */
@Composable
fun RemoteLayout(remote: Remote, showNumbers: Boolean, editing: Boolean, onAddButton: () -> Unit) {
    fun has(roles: List<KeyRole>) = editing || roles.any { remote.button(it) != null }
    fun b(role: KeyRole) = remote.button(role)

    Column(
        Modifier.fillMaxWidth().padding(horizontal = Gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        if (has(listOf(KeyRole.POWER, KeyRole.INPUT, KeyRole.MUTE))) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BeamKey(
                    b(KeyRole.POWER), KeyRole.POWER,
                    Modifier.size(68.dp),
                    container = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.error,
                ) { Icon(Icons.Filled.PowerSettingsNew, null, Modifier.size(30.dp)) }
                Spacer(Modifier.weight(1f))
                LabelledKey(b(KeyRole.INPUT), KeyRole.INPUT, Icons.Filled.Input, stringResource(R.string.key_input))
                Spacer(Modifier.width(18.dp))
                LabelledKey(b(KeyRole.MUTE), KeyRole.MUTE, Icons.AutoMirrored.Filled.VolumeOff, stringResource(R.string.key_mute))
            }
        }

        if (has(dpadRoles)) DPad(remote)

        if (has(navRoles)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LabelledKey(b(KeyRole.BACK), KeyRole.BACK, Icons.AutoMirrored.Filled.Undo, stringResource(R.string.key_back))
                LabelledKey(b(KeyRole.HOME), KeyRole.HOME, Icons.Filled.Home, stringResource(R.string.key_home))
                LabelledKey(b(KeyRole.MENU), KeyRole.MENU, Icons.Filled.Menu, stringResource(R.string.key_menu))
            }
        }

        if (has(rockerRoles)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Rocker(stringResource(R.string.rocker_vol), b(KeyRole.VOL_UP), KeyRole.VOL_UP, b(KeyRole.VOL_DOWN), KeyRole.VOL_DOWN)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (editing || b(KeyRole.INFO) != null || b(KeyRole.GUIDE) != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            LabelledKey(b(KeyRole.INFO), KeyRole.INFO, Icons.Outlined.Info, stringResource(R.string.key_info), size = 52.dp)
                            if (editing || b(KeyRole.GUIDE) != null) {
                                LabelledKey(b(KeyRole.GUIDE), KeyRole.GUIDE, Icons.Filled.ViewList, stringResource(R.string.key_guide), size = 52.dp)
                            }
                        }
                    }
                    LabelledKey(b(KeyRole.EXIT), KeyRole.EXIT, Icons.AutoMirrored.Filled.ExitToApp, stringResource(R.string.key_exit), size = 52.dp)
                }
                Rocker(stringResource(R.string.rocker_ch), b(KeyRole.CH_UP), KeyRole.CH_UP, b(KeyRole.CH_DOWN), KeyRole.CH_DOWN)
            }
        }

        if (has(mediaRoles)) {
            val shown = mediaRoles.filter { editing || b(it) != null }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                shown.forEach { role ->
                    val icon = when (role) {
                        KeyRole.REWIND -> Icons.Filled.FastRewind
                        KeyRole.FAST_FORWARD -> Icons.Filled.FastForward
                        KeyRole.PAUSE -> Icons.Filled.Pause
                        KeyRole.STOP -> Icons.Filled.Stop
                        else -> Icons.Filled.PlayArrow
                    }
                    BeamKey(b(role), role, Modifier.size(50.dp), repeat = role == KeyRole.REWIND || role == KeyRole.FAST_FORWARD) {
                        if (role == KeyRole.PLAY_PAUSE) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                                Icon(Icons.Filled.Pause, null, Modifier.size(18.dp))
                            }
                        } else {
                            Icon(icon, null)
                        }
                    }
                }
            }
        }

        if ((showNumbers || editing) && has(digitRoles)) NumberPad(remote)

        val extras = extraButtons(remote)
        if (extras.isNotEmpty() || editing) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.more_buttons),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3,
                ) {
                    extras.forEach { button ->
                        BeamKey(
                            button, button.role,
                            Modifier.weight(1f).heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp),
                            repeat = true,
                        ) {
                            Text(
                                button.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    }
                    if (editing) {
                        Surface(
                            onClick = onAddButton,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.add_button), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A round key with its name underneath. */
@Composable
private fun LabelledKey(button: RemoteButton?, role: KeyRole, icon: ImageVector, label: String, size: Dp = 56.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BeamKey(button, role, Modifier.size(size)) { Icon(icon, null) }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Arrows around a big OK key. Arrows repeat while held, for scrolling through menus. */
@Composable
private fun DPad(remote: Remote) {
    val size = 236.dp
    val arrow = 72.dp
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.size(size)) {
        Box(Modifier.size(size)) {
            val arrowColor = MaterialTheme.colorScheme.surfaceContainer
            BeamKey(remote.button(KeyRole.UP), KeyRole.UP, Modifier.size(arrow).align(Alignment.TopCenter).padding(top = 4.dp), container = arrowColor, repeat = true) {
                Icon(Icons.Filled.KeyboardArrowUp, null, Modifier.size(32.dp))
            }
            BeamKey(remote.button(KeyRole.DOWN), KeyRole.DOWN, Modifier.size(arrow).align(Alignment.BottomCenter).padding(bottom = 4.dp), container = arrowColor, repeat = true) {
                Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(32.dp))
            }
            BeamKey(remote.button(KeyRole.LEFT), KeyRole.LEFT, Modifier.size(arrow).align(Alignment.CenterStart).padding(start = 4.dp), container = arrowColor, repeat = true) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, Modifier.size(32.dp))
            }
            BeamKey(remote.button(KeyRole.RIGHT), KeyRole.RIGHT, Modifier.size(arrow).align(Alignment.CenterEnd).padding(end = 4.dp), container = arrowColor, repeat = true) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(32.dp))
            }
            BeamKey(
                remote.button(KeyRole.OK), KeyRole.OK,
                Modifier.size(96.dp).align(Alignment.Center),
                container = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(stringResource(R.string.key_ok), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** A tall pill with + at the top and - at the bottom, like a volume or channel rocker. */
@Composable
private fun Rocker(label: String, up: RemoteButton?, upRole: KeyRole, down: RemoteButton?, downRole: KeyRole) {
    Surface(
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.width(76.dp).height(184.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BeamKey(
                up, upRole,
                Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                repeat = true,
            ) { Icon(Icons.Filled.Add, null, Modifier.size(28.dp)) }
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BeamKey(
                down, downRole,
                Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 40.dp, bottomEnd = 40.dp),
                repeat = true,
            ) { Icon(Icons.Filled.Remove, null, Modifier.size(28.dp)) }
        }
    }
}

@Composable
private fun NumberPad(remote: Remote) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(null, 0, null))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { n ->
                    if (n == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val role = KeyRole.digit(n)
                        BeamKey(remote.button(role), role, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                            Text("$n", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}
