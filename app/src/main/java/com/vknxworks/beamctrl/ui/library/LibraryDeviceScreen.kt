package com.vknxworks.beamctrl.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.ir.FlipperIr
import com.vknxworks.beamctrl.ir.LibraryCategory
import com.vknxworks.beamctrl.ir.LibraryDevice
import com.vknxworks.beamctrl.ui.components.MessageView
import com.vknxworks.beamctrl.ui.components.PillButton
import com.vknxworks.beamctrl.ui.components.ScreenTopBar
import com.vknxworks.beamctrl.ui.nav.LocalNavigator
import com.vknxworks.beamctrl.ui.remote.KeyHost
import com.vknxworks.beamctrl.ui.remote.LocalKeyHost
import com.vknxworks.beamctrl.ui.remote.RemoteLayout
import com.vknxworks.beamctrl.ui.theme.Gutter

private sealed interface DeviceState {
    data object Loading : DeviceState
    data class Ready(val device: LibraryDevice, val category: LibraryCategory?, val result: FlipperIr.ImportResult) : DeviceState
    data class Failed(val message: String) : DeviceState
}

/**
 * One remote from the library, shown exactly as it will look once saved. Every button works
 * here already, so it can be tried on the device before saving.
 */
@Composable
fun LibraryDeviceScreen(id: Int) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    var state by remember(id) { mutableStateOf<DeviceState>(DeviceState.Loading) }
    LaunchedEffect(id) {
        state = runCatching {
            val device = c.library.device(id) ?: error("That remote isn't in the library")
            DeviceState.Ready(device, c.library.category(device.category), c.library.load(device))
        }.getOrElse { DeviceState.Failed(it.message ?: "Couldn't open that remote") }
    }

    val ready = state as? DeviceState.Ready
    var name by rememberSaveable(id) { mutableStateOf("") }
    LaunchedEffect(ready?.device?.id) {
        if (ready != null && name.isEmpty()) name = c.remotes.uniqueName(ready.device.title.take(40))
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(ready?.device?.title ?: "Remote")
        when (val s = state) {
            DeviceState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is DeviceState.Failed -> MessageView(Icons.Outlined.ErrorOutline, "Couldn't open this remote", s.message)
            is DeviceState.Ready -> {
                val type = s.category?.type ?: com.vknxworks.beamctrl.data.DeviceType.OTHER
                val preview = remember(s) {
                    Remote(id = "library-$id", name = s.device.title, type = type, brand = s.device.brand, buttons = s.result.buttons)
                }
                CompositionLocalProvider(LocalKeyHost provides KeyHost(holdToRepeat = settings.holdToRepeat, haptics = settings.haptics)) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp),
                    ) {
                        Column(
                            Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                listOfNotNull(s.category?.name, "${s.result.buttons.size} buttons").joinToString("  ·  "),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "Point your phone at the device and try a few buttons. If it responds, save the remote.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (s.result.skipped.isNotEmpty()) {
                                Text(
                                    "${s.result.skipped.size} more buttons use a code format BeamCtrl can't send yet, so they're left out.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(40) },
                                label = { Text("Remote name") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            PillButton(
                                "Save remote",
                                onClick = {
                                    val saved = c.remotes.add(
                                        preview.copy(id = java.util.UUID.randomUUID().toString(), name = name.trim().ifEmpty { s.device.title }),
                                    )
                                    c.telemetry.track("library_browsed", mapOf("action" to "saved", "type" to type.name.lowercase()))
                                    nav.openRemote(saved.id, fromAdd = true)
                                },
                                icon = Icons.Filled.Check,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        RemoteLayout(remote = preview, showNumbers = settings.showNumberPad, editing = false, onAddButton = {})
                    }
                }
            }
        }
    }
}
