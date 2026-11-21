package com.vknxworks.beamctrl.ui.macros

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.R
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.MacroLimits
import com.vknxworks.beamctrl.data.MacroStep
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.ui.components.ChoiceSheet
import com.vknxworks.beamctrl.ui.components.ConfirmDialog
import com.vknxworks.beamctrl.ui.components.CtrlDropdownMenu
import com.vknxworks.beamctrl.ui.components.CtrlMenuItem
import com.vknxworks.beamctrl.ui.components.CtrlSquareButton
import com.vknxworks.beamctrl.ui.components.Group
import com.vknxworks.beamctrl.ui.components.GroupRow
import com.vknxworks.beamctrl.ui.components.NameDialog
import com.vknxworks.beamctrl.ui.components.ScreenTopBar
import com.vknxworks.beamctrl.ui.components.WarningCard
import com.vknxworks.beamctrl.ui.components.topDivider
import com.vknxworks.beamctrl.ui.nav.LocalNavigator
import com.vknxworks.beamctrl.ui.theme.CtrlColors
import com.vknxworks.beamctrl.ui.theme.CtrlType
import com.vknxworks.beamctrl.ui.theme.Gutter
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.foundation.layout.Box

/** "0.5 s", "2 s", "No pause". */
@Composable
fun delayText(ms: Int): String = when {
    ms <= 0 -> stringResource(R.string.macro_no_pause)
    ms % 1000 == 0 -> stringResource(R.string.macro_seconds_whole, ms / 1000)
    else -> stringResource(R.string.macro_seconds_tenths, ms / 1000, (ms % 1000) / 100)
}

/**
 * One macro: its steps in order, each a button on one of your remotes and a pause after it. Run sends them one
 * after another; steps can be added, moved, given another pause or removed.
 */
@Composable
fun MacroScreen(id: String) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val extras by c.extras.extras.collectAsStateWithLifecycle()
    val remotes by c.remotes.remotes.collectAsStateWithLifecycle()
    val running by c.macroRunner.running.collectAsStateWithLifecycle()
    val macro = extras.macros.firstOrNull { it.id == id }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var pickRemote by remember { mutableStateOf(false) }
    var pickButtonFor by remember { mutableStateOf<Remote?>(null) }
    var delayFor by remember { mutableStateOf<Int?>(null) }

    if (macro == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val isRunning = running == macro.id

    fun setSteps(transform: (List<MacroStep>) -> List<MacroStep>) =
        c.extras.updateMacro(id) { it.copy(steps = transform(it.steps).take(MacroLimits.MAX_STEPS)) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(macro.name) {
            if (isRunning) {
                CtrlSquareButton(Icons.Outlined.Stop, stringResource(R.string.macro_stop), onClick = { c.macroRunner.stop() })
            } else {
                CtrlSquareButton(Icons.Outlined.PlayArrow, stringResource(R.string.macro_run), onClick = { c.macroRunner.run(macro) }, enabled = macro.steps.isNotEmpty())
            }
            Box {
                CtrlSquareButton(Icons.Outlined.MoreVert, stringResource(R.string.more), onClick = { menu = true })
                CtrlDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    CtrlMenuItem(stringResource(R.string.rename), onClick = { menu = false; renaming = true }, leadingIcon = Icons.Outlined.DriveFileRenameOutline)
                    CtrlMenuItem(stringResource(R.string.macro_delete), onClick = { menu = false; deleting = true }, leadingIcon = Icons.Outlined.Delete, destructive = true)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            if (!c.blaster.available) {
                WarningCard(stringResource(R.string.no_blaster_banner), Icons.Outlined.WarningAmber, Modifier.padding(horizontal = Gutter, vertical = 8.dp))
            }
            if (macro.steps.isEmpty()) {
                Text(
                    stringResource(R.string.macro_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CtrlColors.Muted,
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 12.dp),
                )
            }
            Group(footer = stringResource(R.string.macro_footer)) {
                macro.steps.forEachIndexed { i, step ->
                    val r = remotes.firstOrNull { it.id == step.remoteId }
                    val b = r?.buttons?.firstOrNull { it.id == step.buttonId }
                    StepRow(
                        number = i + 1,
                        title = stringResource(R.string.dot_pair, r?.name.orEmpty(), b?.label.orEmpty()),
                        subtitle = if (i < macro.steps.lastIndex) stringResource(R.string.macro_then_wait, delayText(step.delayMs)) else null,
                        onClick = { delayFor = i },
                        onUp = if (i > 0) ({ setSteps { s -> s.toMutableList().apply { add(i - 1, removeAt(i)) } } }) else null,
                        onDown = if (i < macro.steps.lastIndex) ({ setSteps { s -> s.toMutableList().apply { add(i + 1, removeAt(i)) } } }) else null,
                        onRemove = { setSteps { s -> s.filterIndexed { j, _ -> j != i } } },
                    )
                }
                if (macro.steps.size < MacroLimits.MAX_STEPS) {
                    GroupRow(
                        stringResource(R.string.macro_add_step),
                        icon = Icons.Outlined.Add,
                        iconTint = CtrlColors.AccentText,
                        titleColor = CtrlColors.AccentText,
                        onClick = { pickRemote = true },
                    )
                }
            }
        }
    }

    if (pickRemote) {
        val withButtons = remotes.filter { it.buttons.isNotEmpty() }.sortedBy { it.name.lowercase() }
        ChoiceSheet(stringResource(R.string.macro_pick_remote), withButtons.map { it to it.name }, null, onDismiss = { pickRemote = false }) { r ->
            pickRemote = false
            pickButtonFor = r
        }
    }
    pickButtonFor?.let { r ->
        ChoiceSheet(stringResource(R.string.macro_pick_button, r.name), r.buttons.map { it to it.label }, null, onDismiss = { pickButtonFor = null }) { b ->
            pickButtonFor = null
            setSteps { it + MacroStep(r.id, b.id, 500) }
        }
    }
    delayFor?.let { i ->
        val current = macro.steps.getOrNull(i)?.delayMs
        ChoiceSheet(
            stringResource(R.string.macro_pause_after),
            MacroLimits.DELAYS_MS.map { it to delayText(it) },
            current,
            onDismiss = { delayFor = null },
        ) { ms ->
            delayFor = null
            setSteps { s -> s.mapIndexed { j, step -> if (j == i) step.copy(delayMs = ms) else step } }
        }
    }
    if (renaming) {
        NameDialog(stringResource(R.string.macro_rename), macro.name, onDismiss = { renaming = false }) { name ->
            c.extras.updateMacro(id) { it.copy(name = name.filterNot { ch -> ch.isISOControl() }.take(80)) }
            renaming = false
        }
    }
    if (deleting) {
        ConfirmDialog(
            stringResource(R.string.macro_delete_title, macro.name),
            stringResource(R.string.macro_delete_body),
            stringResource(R.string.delete),
            onDismiss = { deleting = false },
        ) {
            deleting = false
            if (isRunning) c.macroRunner.stop()
            c.extras.deleteMacro(id)
        }
    }
}

@Composable
private fun StepRow(
    number: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    onUp: (() -> Unit)?,
    onDown: (() -> Unit)?,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .topDivider(CtrlColors.Outline)
            .clickable(onClick = onClick)
            .heightIn(min = 62.dp)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$number", style = CtrlType.Data, color = CtrlColors.Muted)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = CtrlType.Data, color = CtrlColors.Muted)
        }
        CtrlSquareButton(Icons.Outlined.KeyboardArrowUp, stringResource(R.string.move_earlier), onClick = { onUp?.invoke() }, size = 36.dp, enabled = onUp != null)
        CtrlSquareButton(Icons.Outlined.KeyboardArrowDown, stringResource(R.string.move_later), onClick = { onDown?.invoke() }, size = 36.dp, enabled = onDown != null)
        CtrlSquareButton(Icons.Outlined.Close, stringResource(R.string.macro_remove_step), onClick = onRemove, size = 36.dp)
    }
}

/** Names a new macro and creates it; [onCreated] gets its id, to open it. */
@Composable
fun NewMacroDialog(onDismiss: () -> Unit, onCreated: (String) -> Unit) {
    val c = LocalContext.current.container
    NameDialog(stringResource(R.string.macro_new), stringResource(R.string.macro_default_name), confirm = stringResource(R.string.create), onDismiss = onDismiss) { name ->
        val macro = com.vknxworks.beamctrl.data.Macro(name = name.filterNot { it.isISOControl() }.take(80))
        c.extras.addMacro(macro)
        onCreated(macro.id)
    }
}
