package com.vknxworks.beamctrl.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import com.vknxworks.beamctrl.data.lock.LockVault
import com.vknxworks.beamctrl.data.settings.LockTimeout
import com.vknxworks.beamctrl.ui.lock.AppLock
import com.vknxworks.beamctrl.ui.lock.PinRequest
import com.vknxworks.beamctrl.ui.lock.findActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.BuildConfig
import com.vknxworks.beamctrl.R
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.readTextCapped
import com.vknxworks.beamctrl.data.settings.AppSettings
import com.vknxworks.beamctrl.data.settings.ThemeChoice
import com.vknxworks.beamctrl.ui.components.Group
import com.vknxworks.beamctrl.ui.components.GroupChoice
import com.vknxworks.beamctrl.ui.components.GroupRow
import com.vknxworks.beamctrl.ui.components.GroupSwitch
import com.vknxworks.beamctrl.ui.components.LocalBottomClearance
import com.vknxworks.beamctrl.ui.components.TabTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val SPONSOR_URL = "https://github.com/sponsors/VoidedKN0X"
private const val REPO_URL = "https://github.com/VoidedKN0X/BeamCtrl"
private const val ISSUES_URL = "https://github.com/VoidedKN0X/BeamCtrl/issues"
private const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val remotes by c.remotes.remotes.collectAsStateWithLifecycle()
    fun set(transform: (AppSettings) -> AppSettings) {
        scope.launch { c.settings.update(transform) }
    }
    fun open(url: String) {
        try {
            // Opening the browser from here doesn't count as leaving BeamCtrl for the app lock.
            AppLock.expectReturn()
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        }
    }

    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = c.remotes.exportJson()
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) } }.isSuccess
            }
            Toast.makeText(context, context.getString(if (ok) R.string.backup_saved else R.string.backup_save_failed), Toast.LENGTH_SHORT).show()
        }
    }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readTextCapped() } }.getOrNull()
            }
            val added = text?.let { t -> withContext(Dispatchers.Default) { runCatching { c.remotes.importJson(t) }.getOrNull() } }
            Toast.makeText(
                context,
                if (added == null) context.getString(R.string.backup_not_beamctrl)
                else context.resources.getQuantityString(R.plurals.backup_restored, added, added),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 24.dp + LocalBottomClearance.current),
    ) {
        TabTitle(stringResource(R.string.settings))

        Group(stringResource(R.string.group_appearance)) {
            GroupChoice(
                stringResource(R.string.theme),
                ThemeChoice.entries.map { it to stringResource(it.label) },
                settings.theme,
                icon = Icons.Filled.DarkMode,
            ) { t -> set { it.copy(theme = t) } }
            GroupSwitch(
                stringResource(R.string.dynamic_colour),
                stringResource(R.string.dynamic_colour_subtitle),
                checked = settings.dynamicColor,
                icon = Icons.Filled.Palette,
            ) { v -> set { it.copy(dynamicColor = v) } }
        }

        PrivacyAndSecurityGroup(settings)

        Group(
            stringResource(R.string.blaster_group),
            footer = if (c.blaster.available) null else stringResource(R.string.blaster_missing_footer),
        ) {
            GroupRow(
                stringResource(if (c.blaster.available) R.string.blaster_ready else R.string.blaster_missing),
                if (c.blaster.available) stringResource(R.string.blaster_ready_subtitle) else null,
                icon = Icons.Filled.Sensors,
                iconTint = if (c.blaster.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            if (c.blaster.carrierRanges.isNotEmpty()) {
                GroupRow(
                    stringResource(R.string.blaster_frequencies),
                    c.blaster.carrierRanges.joinToString(", ") { r ->
                        if (r.first == r.last) khz(context, r.first) else context.getString(R.string.khz_range, khz(context, r.first), khz(context, r.last))
                    },
                    icon = Icons.Filled.Waves,
                )
            }
        }

        Group(stringResource(R.string.buttons_group)) {
            GroupSwitch(stringResource(R.string.haptics), checked = settings.haptics, icon = Icons.Filled.Vibration) { v -> set { it.copy(haptics = v) } }
            GroupSwitch(
                stringResource(R.string.hold_to_repeat),
                stringResource(R.string.hold_to_repeat_subtitle),
                checked = settings.holdToRepeat,
                icon = Icons.Filled.Repeat,
            ) { v -> set { it.copy(holdToRepeat = v) } }
            GroupSwitch(
                stringResource(R.string.number_pad),
                stringResource(R.string.number_pad_subtitle),
                checked = settings.showNumberPad,
                icon = Icons.Filled.Dialpad,
            ) { v -> set { it.copy(showNumberPad = v) } }
            GroupSwitch(
                stringResource(R.string.keep_screen_on),
                stringResource(R.string.keep_screen_on_subtitle),
                checked = settings.keepScreenOn,
                icon = Icons.Filled.LightMode,
            ) { v -> set { it.copy(keepScreenOn = v) } }
        }

        Group(stringResource(R.string.backup_group), footer = stringResource(R.string.backup_footer)) {
            GroupRow(
                stringResource(R.string.backup_export),
                if (remotes.isEmpty()) stringResource(R.string.backup_export_empty)
                else pluralStringResource(R.plurals.backup_export_subtitle, remotes.size, remotes.size),
                icon = Icons.Filled.Backup,
                onClick = if (remotes.isEmpty()) null else ({ AppLock.expectReturn(); exportBackup.launch("beamctrl-backup.json") }),
            )
            GroupRow(
                stringResource(R.string.backup_restore),
                stringResource(R.string.backup_restore_subtitle),
                icon = Icons.Filled.Restore,
                onClick = {
                    AppLock.expectReturn()
                    restoreBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                },
            )
        }

        Group(stringResource(R.string.group_about)) {
            GroupRow(
                stringResource(R.string.support_title),
                stringResource(R.string.support_subtitle),
                icon = Icons.Filled.Favorite,
                showChevron = true,
                onClick = { open(SPONSOR_URL) },
            )
            GroupRow(
                stringResource(R.string.bugs_title),
                stringResource(R.string.bugs_subtitle),
                icon = Icons.Filled.BugReport,
                showChevron = true,
                onClick = { open(ISSUES_URL) },
            )
            GroupRow(
                stringResource(R.string.source_title),
                stringResource(R.string.source_subtitle),
                icon = Icons.Filled.Code,
                showChevron = true,
                onClick = { open(REPO_URL) },
            )
            GroupRow(
                stringResource(R.string.licence_title),
                stringResource(R.string.licence_subtitle),
                icon = Icons.Filled.Gavel,
                showChevron = true,
                onClick = { open(LICENCE_URL) },
            )
        }

        Text(
            stringResource(R.string.about_footer, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            textAlign = TextAlign.Center,
        )
    }
}

/** Privacy and security: the shared Ctrl series app lock, then hiding BeamCtrl in screenshots. */
@Composable
private fun PrivacyAndSecurityGroup(settings: AppSettings) {
    val context = LocalContext.current
    val c = context.container
    val bioAvailable = remember { AppLock.biometricAvailable(context) }
    val lockOn = c.vault.enabled
    Group(stringResource(R.string.group_privacy), footer = stringResource(R.string.app_lock_footer)) {
        GroupSwitch(
            stringResource(R.string.app_lock),
            stringResource(R.string.app_lock_subtitle),
            checked = lockOn,
            icon = Icons.Outlined.Lock,
        ) { on -> AppLock.request = if (on) PinRequest.ENABLE else PinRequest.DISABLE }
        if (lockOn) {
            GroupRow(
                stringResource(R.string.lock_change_pin),
                stringResource(R.string.lock_change_pin_subtitle, LockVault.PIN_MIN),
                icon = Icons.Outlined.Password,
                showChevron = true,
                onClick = { AppLock.request = PinRequest.CHANGE },
            )
            GroupSwitch(
                stringResource(R.string.lock_fingerprint),
                stringResource(if (bioAvailable || c.vault.biometricEnabled) R.string.lock_fingerprint_subtitle else R.string.lock_fingerprint_unavailable),
                checked = c.vault.biometricEnabled,
                icon = Icons.Outlined.Fingerprint,
            ) { on ->
                if (!on) {
                    c.vault.disableBiometric()
                } else if (bioAvailable) {
                    val activity = context.findActivity()
                    val cipher = c.vault.biometricEnrolCipher()
                    if (activity == null || cipher == null) {
                        Toast.makeText(context, context.getString(R.string.lock_fingerprint_setup_failed), Toast.LENGTH_SHORT).show()
                    } else {
                        AppLock.biometricPrompt(
                            activity,
                            context.getString(R.string.lock_fingerprint_enable_title),
                            context.getString(R.string.lock_fingerprint_enable_subtitle),
                            cipher,
                        ) { unlocked ->
                            if (!c.vault.finishBiometricEnrol(unlocked)) {
                                Toast.makeText(context, context.getString(R.string.lock_fingerprint_setup_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            GroupChoice(
                stringResource(R.string.lock_again),
                LockTimeout.entries.map { it to stringResource(it.label) },
                settings.lockTimeout,
                icon = Icons.Outlined.Timer,
            ) { t -> c.appScope.launch { c.settings.update { it.copy(lockTimeout = t) } } }
        }
        GroupSwitch(
            stringResource(R.string.hide_content),
            stringResource(R.string.hide_content_subtitle),
            checked = settings.hideContent,
            icon = Icons.Outlined.VisibilityOff,
        ) { on -> c.appScope.launch { c.settings.update { it.copy(hideContent = on) } } }
    }
}

private fun khz(context: android.content.Context, hz: Int): String =
    context.getString(R.string.khz, if (hz % 1000 == 0) "${hz / 1000}" else String.format(Locale.getDefault(), "%.1f", hz / 1000.0))
