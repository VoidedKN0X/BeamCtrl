package com.vknxworks.beamctrl.ui.remote

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.R
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.LayoutKind
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.data.SleepTimer
import com.vknxworks.beamctrl.ir.SleepAlarm
import com.vknxworks.beamctrl.ui.components.ChoiceSheet
import com.vknxworks.beamctrl.ui.components.CtrlBottomSheet
import com.vknxworks.beamctrl.ui.components.Group
import com.vknxworks.beamctrl.ui.components.GroupRow
import com.vknxworks.beamctrl.ui.components.NameDialog
import com.vknxworks.beamctrl.ui.lock.AppLock
import com.vknxworks.beamctrl.ui.theme.CtrlColors
import java.text.DateFormat
import java.util.Date

/** Layout: the full remote, or every button in a grid with 2, 3 or 4 per row. */
@Composable
fun LayoutSheet(remote: Remote, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    CtrlBottomSheet(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.layout), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Group(footer = stringResource(R.string.layout_footer)) {
                GroupRow(
                    stringResource(R.string.layout_full),
                    stringResource(R.string.layout_full_sub),
                    icon = Icons.Outlined.SettingsRemote,
                    value = if (remote.layout == LayoutKind.FULL) "✓" else null,
                    onClick = { c.remotes.update(remote.id) { it.copy(layout = LayoutKind.FULL) }; onDismiss() },
                )
                for (n in 2..4) {
                    GroupRow(
                        stringResource(R.string.layout_grid),
                        pluralStringResource(R.plurals.layout_grid_columns, n, n),
                        icon = Icons.Outlined.GridView,
                        value = if (remote.layout == LayoutKind.GRID && remote.gridColumns == n) "✓" else null,
                        onClick = { c.remotes.update(remote.id) { it.copy(layout = LayoutKind.GRID, gridColumns = n) }; onDismiss() },
                    )
                }
            }
        }
    }
}

/** The sleep timer: after a while, press one button of this remote once (usually Power). */
@Composable
fun SleepSheet(remote: Remote, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val extras by c.extras.extras.collectAsStateWithLifecycle()
    val timer = extras.sleep
    val exact = remember { SleepAlarm.exact(context) }
    var buttonId by remember { mutableStateOf((remote.button(KeyRole.POWER) ?: remote.buttons.firstOrNull())?.id) }
    var pickButton by remember { mutableStateOf(false) }
    val button = remote.buttons.firstOrNull { it.id == buttonId }

    CtrlBottomSheet(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.sleep_timer), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (timer != null) {
                val r = c.remotes.get(timer.remoteId)
                val b = r?.buttons?.firstOrNull { it.id == timer.buttonId }
                val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timer.at))
                Group(stringResource(R.string.sleep_active)) {
                    GroupRow(
                        stringResource(R.string.sleep_active_title, b?.label.orEmpty(), r?.name.orEmpty()),
                        stringResource(if (exact) R.string.sleep_at else R.string.sleep_at_window, time),
                        icon = Icons.Outlined.Bedtime,
                    )
                    GroupRow(
                        stringResource(R.string.sleep_cancel),
                        icon = Icons.Outlined.Check,
                        iconTint = CtrlColors.Error,
                        titleColor = CtrlColors.Error,
                        onClick = {
                            SleepAlarm.cancel(context)
                            c.extras.setSleep(null)
                        },
                    )
                }
            }
            if (remote.buttons.isEmpty()) {
                Text(stringResource(R.string.sleep_no_buttons), color = CtrlColors.Muted, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            } else {
                Group(
                    stringResource(if (timer != null) R.string.sleep_new_replaces else R.string.sleep_new),
                    footer = stringResource(if (exact) R.string.sleep_footer_exact else R.string.sleep_footer_window),
                ) {
                    GroupRow(
                        stringResource(R.string.sleep_button),
                        icon = Icons.Outlined.TouchApp,
                        value = button?.label,
                        showChevron = true,
                        onClick = { pickButton = true },
                    )
                    for (minutes in listOf(15, 30, 45, 60, 90, 120)) {
                        GroupRow(
                            pluralStringResource(R.plurals.sleep_after_minutes, minutes, minutes),
                            icon = Icons.Outlined.AccessAlarm,
                            onClick = {
                                val b = button ?: return@GroupRow
                                val at = System.currentTimeMillis() + minutes * 60_000L
                                c.extras.setSleep(SleepTimer(remote.id, b.id, at))
                                SleepAlarm.schedule(context, at)
                                onDismiss()
                            },
                        )
                    }
                    if (!exact) {
                        GroupRow(
                            stringResource(R.string.sleep_allow_exact),
                            stringResource(R.string.sleep_allow_exact_sub),
                            showChevron = true,
                            onClick = {
                                try {
                                    AppLock.expectReturn()
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                } catch (_: ActivityNotFoundException) {
                                }
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
    if (pickButton) {
        ChoiceSheet(
            stringResource(R.string.sleep_button),
            remote.buttons.map { it.id to it.label },
            buttonId,
            onDismiss = { pickButton = false },
        ) { id ->
            buttonId = id
            pickButton = false
        }
    }
}

/**
 * Room picker for a remote: the rooms in use, "No room", and "New room…". Used from the remote's menu and from
 * the long-press sheet on the Remotes tab.
 */
@Composable
fun RoomSheet(remote: Remote, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val remotes by c.remotes.remotes.collectAsStateWithLifecycle()
    val rooms = remember(remotes) { remotes.mapNotNull { it.room }.distinct().sortedBy { it.lowercase() } }
    var naming by remember { mutableStateOf(false) }
    val none = stringResource(R.string.room_none)
    val new = stringResource(R.string.room_new)
    if (!naming) {
        ChoiceSheet(
            stringResource(R.string.room),
            listOf(NONE to none) + rooms.map { it to it } + listOf(NEW to new),
            remote.room ?: NONE,
            onDismiss = onDismiss,
        ) { picked ->
            when (picked) {
                NEW -> naming = true
                NONE -> { c.remotes.update(remote.id) { it.copy(room = null) }; onDismiss() }
                else -> { c.remotes.update(remote.id) { it.copy(room = picked) }; onDismiss() }
            }
        }
    } else {
        NameDialog(stringResource(R.string.room_new_title), "", confirm = stringResource(R.string.save), onDismiss = onDismiss) { name ->
            val clean = name.filterNot { it.isISOControl() }.trim().take(80)
            if (clean.isNotEmpty()) c.remotes.update(remote.id) { it.copy(room = clean) }
            onDismiss()
        }
    }
}

// Picker values that can't be room names (those are trimmed text without control characters).
private const val NONE = "\u0000none"
private const val NEW = "\u0000new"
