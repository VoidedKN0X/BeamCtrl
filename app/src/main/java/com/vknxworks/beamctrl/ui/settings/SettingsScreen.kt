package com.vknxworks.beamctrl.ui.settings

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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.BuildConfig
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

    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = c.remotes.exportJson()
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) } }.isSuccess
            }
            Toast.makeText(context, if (ok) "Backup saved" else "Couldn't save the backup", Toast.LENGTH_SHORT).show()
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
                when (added) {
                    null -> "That isn't a BeamCtrl backup"
                    1 -> "Restored 1 remote"
                    else -> "Restored $added remotes"
                },
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
        TabTitle("Settings")

        Group(
            "IR blaster",
            footer = if (c.blaster.available) null
            else "Android didn't report an IR emitter. Phones with one built in (many Xiaomi, OPPO, OnePlus, Honor and Vivo models) show it here.",
        ) {
            GroupRow(
                if (c.blaster.available) "Ready" else "Not found on this phone",
                if (c.blaster.available) "Buttons send infrared from the top of the phone" else null,
                icon = Icons.Filled.Sensors,
                iconTint = if (c.blaster.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            if (c.blaster.carrierRanges.isNotEmpty()) {
                GroupRow(
                    "Carrier frequencies",
                    c.blaster.carrierRanges.joinToString(", ") { r ->
                        if (r.first == r.last) khz(r.first) else "${khz(r.first)} to ${khz(r.last)}"
                    },
                    icon = Icons.Filled.Waves,
                )
            }
        }

        Group("Buttons") {
            GroupSwitch("Vibrate on press", checked = settings.haptics, icon = Icons.Filled.Vibration) { v -> set { it.copy(haptics = v) } }
            GroupSwitch(
                "Hold to repeat",
                "Keep sending while volume, channel and arrow keys are held",
                checked = settings.holdToRepeat,
                icon = Icons.Filled.Repeat,
            ) { v -> set { it.copy(holdToRepeat = v) } }
            GroupSwitch("Number pad", "Show 0 to 9 on remotes that have them", checked = settings.showNumberPad, icon = Icons.Filled.Dialpad) { v ->
                set { it.copy(showNumberPad = v) }
            }
            GroupSwitch("Keep screen on", "While a remote is open", checked = settings.keepScreenOn, icon = Icons.Filled.LightMode) { v ->
                set { it.copy(keepScreenOn = v) }
            }
        }

        Group("Appearance") {
            GroupChoice("Theme", ThemeChoice.entries.map { it to it.label }, settings.theme, icon = Icons.Filled.DarkMode) { t ->
                set { it.copy(theme = t) }
            }
            GroupSwitch("Wallpaper colours", "Use Material You colours instead of ember", checked = settings.dynamicColor, icon = Icons.Filled.Palette) { v ->
                set { it.copy(dynamicColor = v) }
            }
        }

        Group("Backup", footer = "Remotes are also included in your phone's Android backup, but only when it's end-to-end encrypted (a screen lock is set).") {
            GroupRow(
                "Export all remotes",
                if (remotes.isEmpty()) "Nothing to export yet" else "${remotes.size} remotes to a JSON file",
                icon = Icons.Filled.Backup,
                onClick = if (remotes.isEmpty()) null else ({ exportBackup.launch("beamctrl-backup.json") }),
            )
            GroupRow(
                "Restore from backup",
                "Adds the remotes in a backup file to the ones here",
                icon = Icons.Filled.Restore,
                onClick = { restoreBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            )
        }

        Text(
            "BeamCtrl ${BuildConfig.VERSION_NAME}  ·  VKNXWorks",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            textAlign = TextAlign.Center,
        )
    }
}

private fun khz(hz: Int): String =
    if (hz % 1000 == 0) "${hz / 1000} kHz" else String.format(Locale.getDefault(), "%.1f kHz", hz / 1000.0)
