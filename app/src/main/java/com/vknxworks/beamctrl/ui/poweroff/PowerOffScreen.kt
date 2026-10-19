package com.vknxworks.beamctrl.ui.poweroff

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.RemoteButton
import com.vknxworks.beamctrl.ui.components.ScreenTopBar
import com.vknxworks.beamctrl.ui.nav.LibraryRoute
import com.vknxworks.beamctrl.ui.nav.LocalNavigator
import com.vknxworks.beamctrl.ui.theme.Gutter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pause between codes, so a TV that's busy with the last one doesn't miss the next. */
private const val GAP_MS = 60L

/**
 * Universal power-off: sends every TV power code in the library, most common first, until the
 * TV switches off. When it does, the last few codes can be tried one at a time to find the
 * right one, and its brand looked up in the library.
 */
@Composable
fun PowerOffScreen() {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val codes by produceState<List<RemoteButton>?>(null) { value = runCatching { c.library.tvPowerCodes() }.getOrDefault(emptyList()) }

    var job by remember { mutableStateOf<Job?>(null) }
    var sent by remember { mutableIntStateOf(0) }
    var current by remember { mutableIntStateOf(-1) }
    var finished by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }

    fun start(from: Int) {
        val list = codes ?: return
        finished = false
        // Usage stats: only that it was started, and whether it carried on from where it stopped.
        c.telemetry.track("power_off_used", mapOf("resumed" to (from > 0).toString()))
        job = scope.launch {
            running = true
            try {
                for (i in from until list.size) {
                    current = i
                    c.blaster.sendAndWait(list[i].code)
                    sent = i + 1
                    delay(GAP_MS)
                }
                finished = true
            } finally {
                running = false
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    // Sending stops when you leave, and the screen stays on while it runs. Going to the home
    // screen, switching apps or locking the phone also stops it, so the blaster never keeps
    // going where you can't see it or tap Stop.
    DisposableEffect(Unit) { onDispose { job?.cancel() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stop() }
    if (running) {
        DisposableEffect(Unit) {
            val window = (context as? Activity)?.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Turn off any TV")
        val list = codes
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Gutter, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (!c.blaster.available) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text("This phone doesn't have an IR blaster, so nothing can be sent.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text(
                when {
                    running -> "Keep the top of your phone pointed at the TV. Tap stop as soon as it turns off."
                    finished -> "Sent every code. If the TV didn't turn off, its power code isn't in the library yet."
                    sent > 0 -> "Stopped. If the TV turned off, one of the last codes did it. Try them below to find which."
                    else -> "Sends the power code of every TV in the library, ${list.size} in all, starting with the most common. " +
                        "Point your phone at the TV and start. It takes about a minute to go through them all."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            BigPowerButton(running = running, resume = sent > 0 && !finished, enabled = c.blaster.available && list.isNotEmpty()) {
                when {
                    running -> stop()
                    finished || sent >= list.size -> { sent = 0; start(0) }
                    else -> start(sent)
                }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val progress by animateFloatAsState(if (list.isEmpty()) 0f else sent.toFloat() / list.size, label = "sweep")
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        running && current >= 0 -> "Code ${current + 1} of ${list.size}  ·  ${list[current].label}"
                        sent > 0 -> "$sent of ${list.size} codes sent"
                        else -> "${list.size} codes ready"
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
                if (!running && sent > 0 && !finished) {
                    TextButton(onClick = { sent = 0; current = -1 }) { Text("Start from the beginning") }
                }
            }

            // After stopping: the codes just before the stop, newest first, to pin down the one that worked.
            if (!running && sent > 0) {
                val recent = (maxOf(0, sent - 8) until sent).reversed().map { it to list[it] }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Last codes sent",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            recent.forEach { (i, button) ->
                                RecentCode(
                                    number = i + 1,
                                    button = button,
                                    onTest = { c.blaster.send(button.code) },
                                    onFind = { nav.navigate(LibraryRoute(button.label)) },
                                )
                            }
                        }
                    }
                    Text(
                        "Found it? Tap the search icon to find that brand's full remote in the code library.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BigPowerButton(running: Boolean, resume: Boolean, enabled: Boolean, onClick: () -> Unit) {
    // Breathes while sending, like a remote's LED blinking.
    var pulse by remember { mutableStateOf(false) }
    LaunchedEffect(running) {
        while (running) {
            pulse = !pulse
            delay(400)
        }
        pulse = false
    }
    val scale by animateFloatAsState(if (running && pulse) 1.06f else 1f, label = "pulse")
    val color = if (running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
    val content = if (running) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .scale(scale)
                .size(148.dp)
                .clip(CircleShape)
                .background(if (enabled) color else color.copy(alpha = 0.06f))
                .clickable(enabled = enabled, onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = if (running) "Stop" else if (resume) "Continue" else "Start"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (running) Icons.Filled.Stop else Icons.Filled.PowerSettingsNew, null, Modifier.size(64.dp), tint = content)
        }
        Spacer(Modifier.height(10.dp))
        Text(if (running) "Stop" else if (resume) "Continue" else "Start", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RecentCode(number: Int, button: RemoteButton, onTest: () -> Unit, onFind: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(button.label, style = MaterialTheme.typography.bodyLarge)
            Text("Code $number", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onTest) {
            Icon(Icons.Filled.PowerSettingsNew, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Test")
        }
        androidx.compose.material3.IconButton(onClick = onFind) {
            Icon(Icons.Filled.Search, "Find ${button.label} in the code library")
        }
    }
}
