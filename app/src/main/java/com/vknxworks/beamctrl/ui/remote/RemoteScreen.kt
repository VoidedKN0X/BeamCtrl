package com.vknxworks.beamctrl.ui.remote

import android.app.Activity
import com.vknxworks.beamctrl.R
import androidx.compose.ui.res.stringResource
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.DeviceType
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.RemoteButton
import com.vknxworks.beamctrl.ir.FlipperIr
import com.vknxworks.beamctrl.ui.components.ChoiceSheet
import com.vknxworks.beamctrl.ui.components.ConfirmDialog
import com.vknxworks.beamctrl.ui.components.MessageView
import com.vknxworks.beamctrl.ui.components.NameDialog
import com.vknxworks.beamctrl.ui.components.ScreenTopBar
import com.vknxworks.beamctrl.ui.components.CtrlSquareButton
import com.vknxworks.beamctrl.ui.components.WarningCard
import com.vknxworks.beamctrl.ui.theme.CtrlColors
import com.vknxworks.beamctrl.ui.theme.CtrlShapes
import com.vknxworks.beamctrl.ui.nav.LocalNavigator
import com.vknxworks.beamctrl.ui.theme.Gutter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which button the editor is open for. */
private data class EditTarget(val button: RemoteButton?, val role: KeyRole?)

@Composable
fun RemoteScreen(id: String, startEditing: Boolean) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val remotes by c.remotes.remotes.collectAsStateWithLifecycle()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val remote = remotes.firstOrNull { it.id == id }

    var editing by remember { mutableStateOf(startEditing) }
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var pickingType by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val exportIr = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val r = remote ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(FlipperIr.write(r).toByteArray()) }
                }.isSuccess
            }
            Toast.makeText(context, if (ok) context.getString(R.string.export_saved, r.name) else context.getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
        }
    }

    // Keep the screen awake while a remote is open, if asked to.
    if (settings.keepScreenOn) {
        DisposableEffect(Unit) {
            val window = (context as? Activity)?.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }

    if (remote == null) {
        // Deleted (or never existed): nothing to show.
        LaunchedEffect(Unit) { nav.back() }
        return
    }

    val host = KeyHost(
        editing = editing,
        holdToRepeat = settings.holdToRepeat,
        haptics = settings.haptics,
        onEdit = { button, role -> editTarget = EditTarget(button, role) },
        onSent = {
            c.remotes.markUsed(id)
        },
    )

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(remote.name) {
            BeamLight()
            if (editing) {
                // Done: a small accent button while editing.
                Row(
                    Modifier
                        .height(44.dp)
                        .clip(CtrlShapes.Control)
                        .background(CtrlColors.Accent)
                        .clickable(role = Role.Button) { editing = false }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = CtrlColors.OnAccent)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.done), style = MaterialTheme.typography.labelLarge, color = CtrlColors.OnAccent)
                }
            } else {
                CtrlSquareButton(Icons.Outlined.Edit, stringResource(R.string.edit_buttons), onClick = { editing = true })
            }
            Box {
                CtrlSquareButton(Icons.Outlined.MoreVert, stringResource(R.string.more), onClick = { menu = true })
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    shape = CtrlShapes.Field,
                    containerColor = CtrlColors.Raised,
                    tonalElevation = 0.dp,
                    border = BorderStroke(1.dp, CtrlColors.ControlOutline),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, null) },
                        onClick = { menu = false; renaming = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.device_type)) },
                        leadingIcon = { Icon(Icons.Outlined.Category, null) },
                        onClick = { menu = false; pickingType = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.export_flipper)) },
                        leadingIcon = { Icon(Icons.Outlined.FileDownload, null) },
                        onClick = {
                            menu = false
                            exportIr.launch(remote.name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().replace(' ', '_').ifEmpty { "remote" } + ".ir")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_remote), color = CtrlColors.Error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = CtrlColors.Error) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
            }
        }

        CompositionLocalProvider(LocalKeyHost provides host) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 32.dp),
            ) {
                if (!c.blaster.available) NoBlasterBanner()
                if (editing) {
                    Text(
                        stringResource(R.string.edit_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CtrlColors.Muted,
                        modifier = Modifier.padding(horizontal = Gutter, vertical = 4.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (remote.buttons.isEmpty() && !editing) {
                    Box(Modifier.fillMaxWidth().height(420.dp)) {
                        MessageView(
                            Icons.Outlined.Edit,
                            stringResource(R.string.no_buttons),
                            stringResource(R.string.no_buttons_body),
                            actionLabel = stringResource(R.string.add_buttons),
                            onAction = { editing = true },
                        )
                    }
                } else {
                    RemoteLayout(
                        remote = remote,
                        showNumbers = settings.showNumberPad,
                        editing = editing,
                        onAddButton = { editTarget = EditTarget(null, null) },
                    )
                }
            }
        }
    }

    editTarget?.let { target ->
        ButtonEditorSheet(
            button = target.button,
            presetRole = target.role,
            onDismiss = { editTarget = null },
            onSave = { saved ->
                c.remotes.update(id) { r ->
                    // A button given a spot that's already taken moves the old one to "More buttons".
                    val others = r.buttons.filter { it.id != saved.id }.map {
                        if (saved.role != null && it.role == saved.role) it.copy(role = null) else it
                    }
                    val index = r.buttons.indexOfFirst { it.id == saved.id }
                    val list = others.toMutableList()
                    if (index >= 0) list.add(index.coerceAtMost(list.size), saved) else list.add(saved)
                    r.copy(buttons = list)
                }
                editTarget = null
            },
            onDelete = target.button?.let { b ->
                {
                    c.remotes.update(id) { r -> r.copy(buttons = r.buttons.filter { it.id != b.id }) }
                    editTarget = null
                }
            },
        )
    }

    if (renaming) {
        NameDialog(stringResource(R.string.rename_remote), remote.name, onDismiss = { renaming = false }) { name ->
            c.remotes.update(id) { it.copy(name = name) }
            renaming = false
        }
    }

    if (pickingType) {
        ChoiceSheet(stringResource(R.string.device_type), DeviceType.entries.map { it to stringResource(it.label) }, remote.type, onDismiss = { pickingType = false }) { t ->
            c.remotes.update(id) { it.copy(type = t) }
            pickingType = false
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.delete_remote_title, remote.name),
            stringResource(R.string.delete_remote_body),
            stringResource(R.string.delete),
            onDismiss = { confirmDelete = false },
        ) {
            confirmDelete = false
            c.remotes.delete(id)
        }
    }
}

/** A small light in the top bar that glows each time a signal goes out, like the LED on a real remote. */
@Composable
private fun BeamLight() {
    val blaster = LocalContext.current.container.blaster
    val glow = remember { Animatable(0f) }
    LaunchedEffect(blaster) {
        blaster.beams.collect {
            glow.snapTo(1f)
            launch { glow.animateTo(0f, tween(420)) }
        }
    }
    // A Ctrl status square that flashes in the accent.
    val accent = CtrlColors.Accent
    val idle = CtrlColors.EmptyCell
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .size(22.dp)
            .drawBehind {
                val g = glow.value
                val r = CornerRadius(3.dp.toPx())
                if (g > 0f) drawRoundRect(accent.copy(alpha = 0.30f * g), cornerRadius = CornerRadius(6.dp.toPx()))
                val d = 8.dp.toPx()
                drawRoundRect(
                    lerpColor(idle, accent, g),
                    topLeft = Offset((size.width - d) / 2, (size.height - d) / 2),
                    size = Size(d, d),
                    cornerRadius = r,
                )
            },
    )
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

@Composable
private fun NoBlasterBanner() {
    WarningCard(
        stringResource(R.string.no_blaster_banner),
        Icons.Outlined.WarningAmber,
        Modifier.padding(horizontal = Gutter, vertical = 8.dp),
    )
}
