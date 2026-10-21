package com.vknxworks.beamctrl.ui.remotes

import androidx.compose.foundation.ExperimentalFoundationApi
import com.vknxworks.beamctrl.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.ui.components.ConfirmDialog
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
    val sorted = remember(remotes) { remotes.sortedWith(compareByDescending<Remote> { it.lastUsed }.thenBy { it.created }) }
    var actionsFor by remember { mutableStateOf<Remote?>(null) }
    var renaming by remember { mutableStateOf<Remote?>(null) }
    var deleting by remember { mutableStateOf<Remote?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TabTitle(stringResource(R.string.tab_remotes)) {
            IconButton(onClick = { nav.navigate(AddRoute) }) { Icon(Icons.Filled.Add, stringResource(R.string.add_title)) }
        }
        if (sorted.isEmpty()) {
            MessageView(
                Icons.Filled.SettingsRemote,
                stringResource(R.string.you_no_remotes),
                stringResource(if (c.blaster.available) R.string.remotes_empty_body else R.string.remotes_empty_no_blaster),
                modifier = Modifier.padding(bottom = LocalBottomClearance.current),
                actionLabel = stringResource(R.string.add_title),
                onAction = { nav.navigate(AddRoute) },
            )
        } else {
            CompositionLocalProvider(LocalKeyHost provides KeyHost(holdToRepeat = false, haptics = settings.haptics)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp + LocalBottomClearance.current),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (!c.blaster.available) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.size(12.dp))
                                    Text(stringResource(R.string.remotes_no_blaster), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    items(sorted, key = { it.id }) { remote ->
                        RemoteCard(
                            remote,
                            onOpen = { nav.openRemote(remote.id) },
                            onLongPress = { actionsFor = remote },
                        )
                    }
                }
            }
        }
    }

    actionsFor?.let { r ->
        ModalBottomSheet(onDismissRequest = { actionsFor = null }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Text(r.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Column(Modifier.padding(bottom = 24.dp)) {
                GroupRow(stringResource(R.string.edit_buttons), icon = Icons.Filled.Edit, onClick = { actionsFor = null; nav.openRemote(r.id, edit = true) })
                GroupRow(stringResource(R.string.rename), icon = Icons.Filled.DriveFileRenameOutline, onClick = { actionsFor = null; renaming = r })
                GroupRow(
                    stringResource(R.string.delete),
                    icon = Icons.Filled.Delete,
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { actionsFor = null; deleting = r },
                )
            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RemoteCard(remote: Remote, onOpen: () -> Unit, onLongPress: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val power = remote.button(KeyRole.POWER)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    },
                )
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(remote.type.icon, null, tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.weight(1f))
                if (power != null) {
                    // Quick power without opening the remote.
                    BeamKey(
                        power, KeyRole.POWER,
                        Modifier.size(44.dp),
                        container = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.error,
                        label = stringResource(R.string.remote_power, remote.name),
                    ) { Icon(Icons.Filled.PowerSettingsNew, null, Modifier.size(22.dp)) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(remote.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.buttons_count, remote.buttons.size, remote.buttons.size).let { buttons ->
                    remote.brand?.let { stringResource(R.string.dot_pair, it, buttons) } ?: buttons
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
