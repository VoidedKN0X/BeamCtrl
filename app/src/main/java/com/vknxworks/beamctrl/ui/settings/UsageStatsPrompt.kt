package com.vknxworks.beamctrl.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.container
import kotlinx.coroutines.launch

/** What's shared, in the words used by the first-run question and the Settings footer. */
const val USAGE_STATS_DESCRIPTION =
    "Which screens you open, the kind of device you control (TV, air conditioner and so on), whether you " +
        "browse the code library or use Turn off any TV, whether your phone has an IR blaster, the app version, " +
        "Android version, phone model and language. Never your remotes' names, brands, models or codes, or " +
        "what you search for. Sent to VKNXWorks' own server, not to any analytics company. This is the only " +
        "thing BeamCtrl uses the internet for; everything else works offline."

/** Asked once, when the app first opens. Nothing is sent until the user says yes; they can change it in Settings. */
@Composable
fun UsageStatsPrompt() {
    val c = LocalContext.current.container
    if (!c.telemetry.available) return
    // The stored values, not the placeholder defaults, so the question doesn't flash for people who answered it.
    val stored by c.settings.stored.collectAsStateWithLifecycle(initialValue = null)
    val s = stored ?: return
    if (s.usageStatsAsked) return
    val scope = rememberCoroutineScope()
    fun answer(share: Boolean) {
        scope.launch { c.settings.update { it.copy(usageStats = share, usageStatsAsked = true) } }
    }
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Help improve BeamCtrl?") },
        text = {
            Text(
                "Share anonymous usage stats so I can see how many people use BeamCtrl and which features " +
                    "they use.\n\n$USAGE_STATS_DESCRIPTION\n\nBeamCtrl works exactly the same either way. " +
                    "You can change this any time in Settings.",
            )
        },
        confirmButton = { TextButton(onClick = { answer(true) }) { Text("Share stats") } },
        dismissButton = { TextButton(onClick = { answer(false) }) { Text("No thanks") } },
    )
}
