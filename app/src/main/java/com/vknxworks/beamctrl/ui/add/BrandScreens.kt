package com.vknxworks.beamctrl.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.RemoteButton
import com.vknxworks.beamctrl.ir.BrandTemplate
import com.vknxworks.beamctrl.ir.CodeLibrary
import com.vknxworks.beamctrl.ir.summary
import com.vknxworks.beamctrl.ui.components.MessageView
import com.vknxworks.beamctrl.ui.components.PillButton
import com.vknxworks.beamctrl.ui.components.ScreenTopBar
import com.vknxworks.beamctrl.ui.nav.LocalNavigator
import com.vknxworks.beamctrl.ui.remote.BeamKey
import com.vknxworks.beamctrl.ui.remote.KeyHost
import com.vknxworks.beamctrl.ui.remote.LocalKeyHost
import com.vknxworks.beamctrl.ui.theme.Gutter

/** Test a brand's codes on the TV, then save them as a remote. */
@Composable
fun BrandScreen(id: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val brand = CodeLibrary.brand(id)
    var name by remember { mutableStateOf(brand?.let { c.remotes.uniqueName(it.title) }.orEmpty()) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(brand?.title ?: "Remote")
        if (brand == null) {
            MessageView(Icons.Filled.PowerSettingsNew, "That brand isn't in the list")
            return@Column
        }
        CompositionLocalProvider(LocalKeyHost provides KeyHost(holdToRepeat = settings.holdToRepeat, haptics = settings.haptics)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = Gutter, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(brand.note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Point the top of your phone at the TV and try a few buttons. If the TV responds, save the remote.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                TestKeys(brand)
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
                        val remote = c.remotes.add(brand.toRemote(name.trim().ifEmpty { brand.title }))
                        nav.openRemote(remote.id, fromAdd = true)
                    },
                    icon = Icons.Filled.Check,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Codes: ${brand.keys.first().second.summary.substringBefore("  ")}, ${brand.keys.size} buttons. You can change any of them later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Power, volume and mute for trying a brand's codes. */
@Composable
private fun TestKeys(brand: BrandTemplate) {
    val tests: List<Triple<KeyRole, ImageVector, String>> = listOf(
        Triple(KeyRole.POWER, Icons.Filled.PowerSettingsNew, "Power"),
        Triple(KeyRole.VOL_DOWN, Icons.AutoMirrored.Filled.VolumeDown, "Vol −"),
        Triple(KeyRole.VOL_UP, Icons.AutoMirrored.Filled.VolumeUp, "Vol +"),
        Triple(KeyRole.MUTE, Icons.AutoMirrored.Filled.VolumeOff, "Mute"),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        tests.forEach { (role, icon, label) ->
            val code = brand.code(role)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val isPower = role == KeyRole.POWER
                BeamKey(
                    code?.let { RemoteButton(id = "${brand.id}-${role.name}", label = label, role = role, code = it) },
                    role,
                    Modifier.size(64.dp),
                    container = if (isPower) MaterialTheme.colorScheme.error.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (isPower) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    repeat = role == KeyRole.VOL_UP || role == KeyRole.VOL_DOWN,
                ) { Icon(icon, null, Modifier.size(28.dp)) }
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Steps through every brand's power code until the TV responds. */
@Composable
fun FindBrandScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val brands = CodeLibrary.brands
    var index by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Find my TV")
        if (index >= brands.size) {
            MessageView(
                Icons.Filled.PowerSettingsNew,
                "No match",
                "None of the built-in brands worked. If you have a Flipper .ir file for your TV you can import it, " +
                    "or build a remote yourself from its codes.",
                actionLabel = "Start again",
                onAction = { index = 0 },
            )
            return@Column
        }
        val brand = brands[index]
        val power = brand.code(KeyRole.POWER)
        CompositionLocalProvider(LocalKeyHost provides KeyHost(holdToRepeat = false, haptics = settings.haptics)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = Gutter, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    "Brand ${index + 1} of ${brands.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(brand.brand, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                Text(
                    "Point your phone at the TV and tap power. Did the TV turn off or on?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                BeamKey(
                    power?.let { RemoteButton(id = "find-${brand.id}", label = "Power", role = KeyRole.POWER, code = it) },
                    KeyRole.POWER,
                    Modifier.size(120.dp),
                    container = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.error,
                ) { Icon(Icons.Filled.PowerSettingsNew, null, Modifier.size(52.dp)) }
                Spacer(Modifier.height(8.dp))
                PillButton(
                    "It worked",
                    onClick = { nav.navigate(com.vknxworks.beamctrl.ui.nav.BrandRoute(brand.id)) },
                    icon = Icons.Filled.Check,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { index++ }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Filled.SkipNext, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (index == brands.lastIndex) "Didn't work" else "Didn't work, try ${brands[index + 1].brand}")
                }
            }
        }
    }
}
