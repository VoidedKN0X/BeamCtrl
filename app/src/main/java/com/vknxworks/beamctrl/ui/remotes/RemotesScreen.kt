package com.vknxworks.beamctrl.ui.remotes

import com.vknxworks.beamctrl.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import com.vknxworks.beamctrl.data.Macro
import com.vknxworks.beamctrl.ui.macros.NewMacroDialog
import com.vknxworks.beamctrl.ui.nav.MacroRoute
import com.vknxworks.beamctrl.ui.remote.RoomSheet
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.ui.components.ConfirmDialog
import com.vknxworks.beamctrl.ui.components.CtrlBottomSheet
import com.vknxworks.beamctrl.ui.components.CtrlHeroCard
import com.vknxworks.beamctrl.ui.components.CtrlSquareButton
import com.vknxworks.beamctrl.ui.components.WarningCard
import com.vknxworks.beamctrl.ui.components.topDivider
import com.vknxworks.beamctrl.ui.theme.CtrlColors
import com.vknxworks.beamctrl.ui.theme.CtrlShapes
import com.vknxworks.beamctrl.ui.theme.CtrlType
import com.vknxworks.beamctrl.ui.components.GroupRow
import com.vknxworks.beamctrl.ui.components.LocalBottomClearance
import com.vknxworks.beamctrl.ui.components.MessageView
import com.vknxworks.beamctrl.ui.components.NameDialog
import com.vknxworks.beamctrl.ui.components.TabTitle
import com.vknxworks.beamctrl.ui.nav.AddRoute
import com.vknxworks.beamctrl.ui.nav.LocalNavigator
import com.vknxworks.beamctrl.ui.remote.BeamKey
import com.vknxworks.beamctrl.ui.remote.KeyHost
import com.vknxworks.beamctrl.ui.remote.LocalKeyHost
import com.vknxworks.beamctrl.ui.theme.Gutter

/** The Remotes tab: every saved remote, most recently used first, each with a quick power key. */
@Composable
fun RemotesScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val remotes by c.remotes.remotes.collectAsStateWithLifecycle()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val extras by c.extras.extras.collectAsStateWithLifecycle()
    val running by c.macroRunner.running.collectAsStateWithLifecycle()
    val rooms = remember(remotes) { remotes.mapNotNull { it.room }.distinct().sortedBy { it.lowercase() } }
    var roomFilter by rememberSaveable { mutableStateOf<String?>(null) }
    // A room that no longer has remotes stops being the filter.
    val room = roomFilter?.takeIf { it in rooms }
    val sorted = remember(remotes, room) {
        remotes.filter { room == null || it.room == room }
            .sortedWith(compareByDescending<Remote> { it.lastUsed }.thenBy { it.created })
    }
    var actionsFor by remember { mutableStateOf<Remote?>(null) }
    var roomFor by remember { mutableStateOf<Remote?>(null) }
    var newMacro by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Remote?>(null) }
    var deleting by remember { mutableStateOf<Remote?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        if (remotes.isEmpty()) {
            TabTitle(stringResource(R.string.tab_remotes)) {
                CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.add_title), onClick = { nav.navigate(AddRoute) })
            }
            MessageView(
                Icons.Outlined.SettingsRemote,
                stringResource(R.string.you_no_remotes),
                stringResource(if (c.blaster.available) R.string.remotes_empty_body else R.string.remotes_empty_no_blaster),
                modifier = Modifier.padding(bottom = LocalBottomClearance.current),
                actionLabel = stringResource(R.string.add_title),
                onAction = { nav.navigate(AddRoute) },
            )
        } else {
            CompositionLocalProvider(LocalKeyHost provides KeyHost(holdToRepeat = false, haptics = settings.haptics)) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "title") {
                        TabTitle(
                            stringResource(R.string.tab_remotes),
                            label = pluralStringResource(R.plurals.you_remote_count, remotes.size, remotes.size),
                        ) {
                            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.add_title), onClick = { nav.navigate(AddRoute) })
                        }
                    }
                    if (rooms.isNotEmpty()) {
                        item(key = "rooms") {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RoomChip(stringResource(R.string.room_all), room == null) { roomFilter = null }
                                rooms.forEach { r -> RoomChip(r, room == r) { roomFilter = r } }
                            }
                        }
                    }
                    if (extras.macros.isNotEmpty()) {
                        item(key = "macros-header") {
                            Row(
                                Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 14.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(R.string.macros), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                                CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.macro_new), onClick = { newMacro = true }, size = 36.dp)
                            }
                        }
                        items(extras.macros, key = { "macro-" + it.id }) { m ->
                            MacroRow(
                                m,
                                running = running == m.id,
                                onOpen = { nav.navigate(MacroRoute(m.id)) },
                                onRun = { if (running == m.id) c.macroRunner.stop() else c.macroRunner.run(m) },
                            )
                        }
                    }
                    if (!c.blaster.available) {
                        item(key = "no-blaster") {
                            WarningCard(
                                stringResource(R.string.remotes_no_blaster),
                                Icons.Outlined.WarningAmber,
                                Modifier.padding(horizontal = Gutter, vertical = 6.dp),
                            )
                        }
                    }
                    // The hero: the remote used last, with its power key right on the card.
                    val first = sorted.firstOrNull()
                    if (first != null) item(key = "hero-${first.id}") {
                        HeroRemote(
                            first,
                            onOpen = { nav.openRemote(first.id) },
                            onLongPress = { actionsFor = first },
                            modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                        )
                    }
                    if (sorted.size > 1) {
                        item(key = "all-header") {
                            Row(
                                Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 18.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(R.string.label_all_remotes), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                                Text(
                                    pluralStringResource(R.plurals.you_remote_count, sorted.size, sorted.size).uppercase(),
                                    style = CtrlType.Data,
                                    color = CtrlColors.Muted,
                                )
                            }
                        }
                        items(sorted.drop(1), key = { it.id }) { remote ->
                            RemoteRow(
                                remote,
                                onOpen = { nav.openRemote(remote.id) },
                                onLongPress = { actionsFor = remote },
                            )
                        }
                    }
                }
            }
        }
    }

    actionsFor?.let { r ->
        CtrlBottomSheet(onDismissRequest = { actionsFor = null }) {
            Text(r.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Column(Modifier.padding(bottom = 24.dp)) {
                GroupRow(stringResource(R.string.edit_buttons), icon = Icons.Outlined.Edit, onClick = { actionsFor = null; nav.openRemote(r.id, edit = true) })
                GroupRow(stringResource(R.string.rename), icon = Icons.Outlined.DriveFileRenameOutline, onClick = { actionsFor = null; renaming = r })
                GroupRow(stringResource(R.string.room), icon = Icons.Outlined.MeetingRoom, value = r.room, onClick = { actionsFor = null; roomFor = r })
                GroupRow(
                    stringResource(R.string.delete),
                    icon = Icons.Outlined.Delete,
                    iconTint = CtrlColors.Error,
                    titleColor = CtrlColors.Error,
                    onClick = { actionsFor = null; deleting = r },
                )
            }
        }
    }
    roomFor?.let { r -> RoomSheet(r, onDismiss = { roomFor = null }) }
    if (newMacro) {
        NewMacroDialog(onDismiss = { newMacro = false }) { id ->
            newMacro = false
            nav.navigate(MacroRoute(id))
        }
    }
    renaming?.let { r ->
        NameDialog(stringResource(R.string.rename_remote), r.name, onDismiss = { renaming = null }) { name ->
            c.remotes.update(r.id) { it.copy(name = name) }
            renaming = null
        }
    }
    deleting?.let { r ->
        ConfirmDialog(stringResource(R.string.delete_remote_title, r.name), stringResource(R.string.delete_remote_body), stringResource(R.string.delete), onDismiss = { deleting = null }) {
            c.remotes.delete(r.id)
            deleting = null
        }
    }
}

/** Brand and button count, e.g. "Samsung · 42 buttons". */
@Composable
private fun remoteMeta(remote: Remote): String =
    pluralStringResource(R.plurals.buttons_count, remote.buttons.size, remote.buttons.size).let { buttons ->
        val withBrand = remote.brand?.let { stringResource(R.string.dot_pair, it, buttons) } ?: buttons
        remote.room?.let { stringResource(R.string.dot_pair, it, withBrand) } ?: withBrand
    }

@Composable
private fun RoomChip(text: String, selected: Boolean, onClick: () -> Unit) {
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

/** One macro on the Remotes tab: its name, how many steps, and Run (or Stop while it runs). */
@Composable
private fun MacroRow(macro: Macro, running: Boolean, onOpen: () -> Unit, onRun: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = Gutter)
            .topDivider()
            .heightIn(min = 60.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.PlaylistPlay, null, Modifier.size(22.dp), tint = CtrlColors.Muted)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(macro.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pluralStringResource(R.plurals.macro_steps, macro.steps.size, macro.steps.size), style = CtrlType.Data, color = CtrlColors.Muted)
        }
        CtrlSquareButton(
            if (running) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
            stringResource(if (running) R.string.macro_stop else R.string.macro_run_named, macro.name),
            onClick = onRun,
            enabled = macro.steps.isNotEmpty(),
        )
    }
}

/** The last-used remote as an accent hero card: type and button count on top, its name big, and quick power. */
@Composable
private fun HeroRemote(remote: Remote, onOpen: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val power = remote.button(KeyRole.POWER)
    CtrlHeroCard(
        label = stringResource(R.string.dot_pair, stringResource(R.string.label_last_used), stringResource(remote.type.label)),
        meta = pluralStringResource(R.plurals.buttons_count, remote.buttons.size, remote.buttons.size),
        onClick = onOpen,
        onLongClick = onLongPress,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    remote.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = CtrlColors.OnAccent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (remote.brand != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(remote.brand.uppercase(), style = CtrlType.Data, color = CtrlColors.OnAccent.copy(alpha = 0.7f), maxLines = 1)
                }
            }
            if (power != null) {
                Spacer(Modifier.width(12.dp))
                // Quick power without opening the remote: a dark key on the accent.
                BeamKey(
                    power, KeyRole.POWER,
                    Modifier.size(64.dp),
                    shape = CtrlShapes.Key,
                    container = CtrlColors.OnAccent,
                    contentColor = CtrlColors.Accent,
                    borderColor = null,
                    label = context.getString(R.string.remote_power, remote.name),
                ) { Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(28.dp)) }
            }
        }
    }
}

/** One remote in the list: type icon, name and details, and a quick power key. */
@Composable
private fun RemoteRow(remote: Remote, onOpen: () -> Unit, onLongPress: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val power = remote.button(KeyRole.POWER)
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            )
            .padding(horizontal = Gutter)
            .topDivider()
            .heightIn(min = 64.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(remote.type.icon, null, Modifier.size(22.dp), tint = CtrlColors.Muted)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(remote.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(remoteMeta(remote), style = CtrlType.Data, color = CtrlColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (power != null) {
            Spacer(Modifier.width(12.dp))
            BeamKey(
                power, KeyRole.POWER,
                Modifier.size(44.dp),
                shape = CtrlShapes.Control,
                label = stringResource(R.string.remote_power, remote.name),
            ) { Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(20.dp)) }
        }
    }
}
