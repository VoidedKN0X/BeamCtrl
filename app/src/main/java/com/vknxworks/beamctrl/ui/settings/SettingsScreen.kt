package com.vknxworks.beamctrl.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.R
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.lock.LockVault
import com.vknxworks.beamctrl.data.settings.AppSettings
import com.vknxworks.beamctrl.data.settings.LockTimeout
import com.vknxworks.beamctrl.ui.components.Group
import com.vknxworks.beamctrl.ui.components.GroupChoice
import com.vknxworks.beamctrl.ui.components.GroupRow
import com.vknxworks.beamctrl.ui.components.GroupSwitch
import com.vknxworks.beamctrl.ui.components.ScreenTopBar
import com.vknxworks.beamctrl.ui.lock.AppLock
import com.vknxworks.beamctrl.ui.lock.PinRequest
import com.vknxworks.beamctrl.ui.lock.findActivity
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Settings, opened from the You page (tap the profile picture). Backup and About are on the You page itself.
 * BeamCtrl is dark only (Ctrl / Signal), so there are no theme or wallpaper colour settings.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    fun set(transform: (AppSettings) -> AppSettings) {
        scope.launch { c.settings.update(transform) }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.settings))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            PrivacyAndSecurityGroup(settings)

            Group(
                stringResource(R.string.blaster_group),
                footer = if (c.blaster.available) null else stringResource(R.string.blaster_missing_footer),
            ) {
                GroupRow(
                    stringResource(if (c.blaster.available) R.string.blaster_ready else R.string.blaster_missing),
                    if (c.blaster.available) stringResource(R.string.blaster_ready_subtitle) else null,
                    icon = Icons.Outlined.Sensors,
                    iconTint = if (c.blaster.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    trailing = {
                        com.vknxworks.beamctrl.ui.components.StatusTag(
                            stringResource(if (c.blaster.available) R.string.status_ready else R.string.status_missing),
                            active = c.blaster.available,
                        )
                    },
                )
                if (c.blaster.carrierRanges.isNotEmpty()) {
                    GroupRow(
                        stringResource(R.string.blaster_frequencies),
                        c.blaster.carrierRanges.joinToString(", ") { r ->
                            if (r.first == r.last) khz(context, r.first) else context.getString(R.string.khz_range, khz(context, r.first), khz(context, r.last))
                        },
                        icon = Icons.Outlined.Waves,
                    )
                }
            }

            Group(stringResource(R.string.buttons_group)) {
                GroupSwitch(stringResource(R.string.haptics), checked = settings.haptics, icon = Icons.Outlined.Vibration) { v -> set { it.copy(haptics = v) } }
                GroupSwitch(
                    stringResource(R.string.hold_to_repeat),
                    stringResource(R.string.hold_to_repeat_subtitle),
                    checked = settings.holdToRepeat,
                    icon = Icons.Outlined.Repeat,
                ) { v -> set { it.copy(holdToRepeat = v) } }
                GroupSwitch(
                    stringResource(R.string.number_pad),
                    stringResource(R.string.number_pad_subtitle),
                    checked = settings.showNumberPad,
                    icon = Icons.Outlined.Dialpad,
                ) { v -> set { it.copy(showNumberPad = v) } }
                GroupSwitch(
                    stringResource(R.string.keep_screen_on),
                    stringResource(R.string.keep_screen_on_subtitle),
                    checked = settings.keepScreenOn,
                    icon = Icons.Outlined.LightMode,
                ) { v -> set { it.copy(keepScreenOn = v) } }
            }
        }
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
            stringResource(R.string.hide_screenshots),
            stringResource(R.string.hide_screenshots_subtitle),
            checked = settings.hideScreenshots,
            icon = Icons.Outlined.VisibilityOff,
        ) { on -> c.appScope.launch { c.settings.update { it.copy(hideScreenshots = on) } } }
        GroupSwitch(
            stringResource(R.string.hide_recents),
            stringResource(R.string.hide_recents_subtitle),
            checked = settings.hideRecents,
            icon = Icons.Outlined.Layers,
        ) { on -> c.appScope.launch { c.settings.update { it.copy(hideRecents = on) } } }
    }
}

private fun khz(context: android.content.Context, hz: Int): String =
    context.getString(R.string.khz, if (hz % 1000 == 0) "${hz / 1000}" else String.format(Locale.getDefault(), "%.1f", hz / 1000.0))
