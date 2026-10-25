package com.vknxworks.beamctrl.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.BuildConfig
import com.vknxworks.beamctrl.R
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.Backup
import com.vknxworks.beamctrl.data.BackupException
import com.vknxworks.beamctrl.data.BackupFile
import com.vknxworks.beamctrl.data.FileTooBigException
import com.vknxworks.beamctrl.data.readTextCapped
import com.vknxworks.beamctrl.ui.components.ConfirmDialog
import com.vknxworks.beamctrl.ui.components.Group
import com.vknxworks.beamctrl.ui.components.GroupRow
import com.vknxworks.beamctrl.ui.components.CtrlAlertDialog
import com.vknxworks.beamctrl.ui.components.CtrlButton
import com.vknxworks.beamctrl.ui.components.CtrlSquareButton
import com.vknxworks.beamctrl.ui.components.SectionLabel
import com.vknxworks.beamctrl.ui.theme.CtrlColors
import com.vknxworks.beamctrl.ui.theme.CtrlType
import com.vknxworks.beamctrl.ui.lock.AppLock
import com.vknxworks.beamctrl.ui.nav.LocalNavigator
import com.vknxworks.beamctrl.ui.nav.SettingsRoute
import com.vknxworks.beamctrl.ui.theme.Gutter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

private const val SPONSOR_URL = "https://github.com/sponsors/VoidedKN0X"
private const val REPO_URL = "https://github.com/VoidedKN0X/BeamCtrl"
private const val ISSUES_URL = "https://github.com/VoidedKN0X/BeamCtrl/issues"
private const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/**
 * The You page, opened from the profile picture on the Remotes and Add tabs. BeamCtrl has no accounts, so
 * the card at the top is this phone. Then Backup (the only way to move remotes to another phone), the way
 * into Settings, and the About section every Ctrl app ends with.
 */
@Composable
fun YouScreen() {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val remotes by c.remotes.remotes.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<Backup?>(null) }
    var problem by remember { mutableStateOf<Int?>(null) }
    val deviceName = remember {
        runCatching { android.provider.Settings.Global.getString(context.contentResolver, android.provider.Settings.Global.DEVICE_NAME) }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: listOf(Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, Build.MODEL).distinct().joinToString(" ").trim()
    }

    fun open(url: String) {
        try {
            // Opening the browser from here doesn't count as leaving BeamCtrl for the app lock.
            AppLock.expectReturn()
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        }
    }

    val backUp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = BackupFile.write(c.remotes.remotes.value, c.settings.stored.first())
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("No file")
                    out.use { it.write(text.toByteArray()) }
                }.isSuccess
            }
            Toast.makeText(context, context.getString(if (ok) R.string.backup_saved else R.string.backup_save_failed), Toast.LENGTH_SHORT).show()
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            // The whole file is read and checked first; nothing on the phone changes until it's confirmed.
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.use { it.readTextCapped() }
                        ?: throw BackupException(R.string.backup_error_unreadable)
                    BackupFile.read(text)
                }
            }
            result
                .onSuccess { pending = it }
                .onFailure { e ->
                    problem = when (e) {
                        is BackupException -> e.reason
                        is FileTooBigException -> R.string.backup_error_too_big
                        else -> R.string.backup_error_unreadable
                    }
                }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Close button and a mono PROFILE label, like every Ctrl app's profile page.
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Gutter, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CtrlSquareButton(Icons.Outlined.Close, stringResource(R.string.back), onClick = nav::back)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SectionLabel(stringResource(R.string.label_profile)) }
            Spacer(Modifier.size(44.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(68.dp).clip(RoundedCornerShape(22.dp)).background(CtrlColors.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PhoneAndroid, null, Modifier.size(30.dp), tint = CtrlColors.OnAccent)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        deviceName.ifBlank { stringResource(R.string.you_this_phone) },
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOf(
                            if (remotes.isEmpty()) stringResource(R.string.you_no_remotes)
                            else pluralStringResource(R.plurals.you_remote_count, remotes.size, remotes.size),
                            stringResource(if (c.blaster.available) R.string.you_blaster_ready else R.string.you_blaster_missing),
                        ).joinToString(" · "),
                        style = CtrlType.Data,
                        color = CtrlColors.Muted,
                    )
                }
            }

            Group(stringResource(R.string.backup_group), footer = stringResource(R.string.backup_footer)) {
                GroupRow(
                    stringResource(R.string.backup_to_file),
                    if (remotes.isEmpty()) stringResource(R.string.backup_to_file_subtitle_empty)
                    else pluralStringResource(R.plurals.backup_to_file_subtitle, remotes.size, remotes.size),
                    icon = Icons.Outlined.Backup,
                    showChevron = true,
                    onClick = {
                        // Android's file picker doesn't count as leaving BeamCtrl for the app lock.
                        AppLock.expectReturn()
                        backUp.launch(context.getString(R.string.backup_file_name, LocalDate.now().toString()))
                    },
                )
                GroupRow(
                    stringResource(R.string.backup_restore),
                    stringResource(R.string.backup_restore_subtitle),
                    icon = Icons.Outlined.Restore,
                    showChevron = true,
                    onClick = {
                        AppLock.expectReturn()
                        restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    },
                )
            }

            Group(stringResource(R.string.label_app)) {
                GroupRow(
                    stringResource(R.string.settings),
                    stringResource(R.string.settings_subtitle),
                    icon = Icons.Outlined.Tune,
                    showChevron = true,
                    onClick = { nav.navigate(SettingsRoute) },
                )
            }

            Group(stringResource(R.string.group_about)) {
                GroupRow(
                    stringResource(R.string.bugs_title),
                    stringResource(R.string.bugs_subtitle),
                    icon = Icons.Outlined.BugReport,
                    showChevron = true,
                    onClick = { open(ISSUES_URL) },
                )
                GroupRow(
                    stringResource(R.string.source_title),
                    stringResource(R.string.source_subtitle),
                    icon = Icons.Outlined.Code,
                    showChevron = true,
                    onClick = { open(REPO_URL) },
                )
                GroupRow(
                    stringResource(R.string.licence_title),
                    stringResource(R.string.licence_subtitle),
                    icon = Icons.Outlined.Gavel,
                    value = "GPL-3.0",
                    onClick = { open(LICENCE_URL) },
                )
            }

            Column(
                Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CtrlButton(
                    stringResource(R.string.support_title),
                    onClick = { open(SPONSOR_URL) },
                    icon = Icons.Outlined.FavoriteBorder,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.label_footer_badges).uppercase(),
                    style = CtrlType.Label.copy(letterSpacing = 0.12.em),
                    color = CtrlColors.Muted,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.about_footer, BuildConfig.VERSION_NAME),
                    style = CtrlType.Data,
                    color = CtrlColors.Muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    pending?.let { backup ->
        val inFile = pluralStringResource(R.plurals.you_remote_count, backup.remotes.size, backup.remotes.size)
        val here = pluralStringResource(R.plurals.you_remote_count, remotes.size, remotes.size)
        val body = if (backup.settings != null && backup.created != null) {
            val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(backup.created))
            stringResource(R.string.backup_restore_confirm_body, date, inFile, here)
        } else {
            stringResource(R.string.backup_restore_confirm_body_v1, inFile, here)
        }
        ConfirmDialog(
            title = stringResource(R.string.backup_restore_confirm_title),
            body = body,
            confirm = stringResource(R.string.backup_restore_confirm),
            onDismiss = { pending = null },
        ) {
            pending = null
            c.remotes.replaceAll(backup.remotes)
            val settings = backup.settings
            if (settings != null) c.appScope.launch { c.settings.update { settings.applyTo(it) } }
            val count = backup.remotes.size
            Toast.makeText(
                context,
                context.resources.getQuantityString(
                    if (settings != null) R.plurals.backup_restored_all else R.plurals.backup_restored,
                    count,
                    count,
                ),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    problem?.let { reason ->
        CtrlAlertDialog(
            onDismissRequest = { problem = null },
            title = { Text(stringResource(R.string.backup_restore_failed_title)) },
            text = { Text(stringResource(reason)) },
            confirmButton = { TextButton(onClick = { problem = null }) { Text(stringResource(R.string.lock_ok)) } },
        )
    }
}
