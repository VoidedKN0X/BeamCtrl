package com.vknxworks.beamctrl.ui.add

import com.vknxworks.beamctrl.ui.components.ctrlFieldColors
import com.vknxworks.beamctrl.ui.components.CtrlSecondaryButton
import com.vknxworks.beamctrl.ui.theme.CtrlShapes
import com.vknxworks.beamctrl.ui.theme.CtrlColors
import androidx.compose.foundation.layout.Arrangement
import com.vknxworks.beamctrl.ir.protocolLabel
import com.vknxworks.beamctrl.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.vknxworks.beamctrl.ui.components.MessageView
import com.vknxworks.beamctrl.ui.components.PillButton
import com.vknxworks.beamctrl.ui.components.ScreenTopBar
import com.vknxworks.beamctrl.ui.nav.LocalNavigator
import com.vknxworks.beamctrl.ui.remote.BeamKey
import com.vknxworks.beamctrl.ui.remote.KeyHost
import com.vknxworks.beamctrl.ui.remote.LocalKeyHost
import com.vknxworks.beamctrl.ui.theme.Gutter
import com.vknxworks.beamctrl.ui.theme.CtrlType
import com.vknxworks.beamctrl.ui.components.SectionLabel

/** Test a brand's codes on the TV, then save them as a remote. */
@Composable
fun BrandScreen(id: String) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val brand = CodeLibrary.brand(id)
    var name by remember { mutableStateOf(brand?.let { c.remotes.uniqueName(it.title(context)) }.orEmpty()) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(brand?.title(context) ?: stringResource(R.string.remote_unnamed))
        if (brand == null) {
            MessageView(Icons.Outlined.PowerSettingsNew, stringResource(R.string.brand_missing))
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
                Text(stringResource(brand.note), style = MaterialTheme.typography.bodyLarge, color = CtrlColors.Muted)
                Text(
                    stringResource(R.string.brand_try),
                    style = MaterialTheme.typography.bodyLarge,
                )
                TestKeys(brand)
                OutlinedTextField(
                    shape = CtrlShapes.Field,
                    colors = ctrlFieldColors(),
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text(stringResource(R.string.remote_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                PillButton(
                    stringResource(R.string.save_remote),
                    onClick = {
                        val remote = c.remotes.add(brand.toRemote(context, name.trim().ifEmpty { brand.title(context) }))
                        nav.openRemote(remote.id, fromAdd = true)
                    },
                    icon = Icons.Outlined.Check,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    pluralStringResource(R.plurals.brand_codes, brand.keys.size, brand.keys.first().second.protocolLabel(context), brand.keys.size),
                    style = CtrlType.Data,
                    color = CtrlColors.Muted,
                )
            }
        }
    }
}

/** Power, volume and mute for trying a brand's codes. */
@Composable
private fun TestKeys(brand: BrandTemplate) {
    val tests: List<Triple<KeyRole, ImageVector, String>> = listOf(
        Triple(KeyRole.POWER, Icons.Outlined.PowerSettingsNew, stringResource(R.string.key_power)),
        Triple(KeyRole.VOL_DOWN, Icons.AutoMirrored.Outlined.VolumeDown, stringResource(R.string.key_vol_down_short)),
        Triple(KeyRole.VOL_UP, Icons.AutoMirrored.Outlined.VolumeUp, stringResource(R.string.key_vol_up_short)),
        Triple(KeyRole.MUTE, Icons.AutoMirrored.Outlined.VolumeOff, stringResource(R.string.key_mute)),
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
                    container = if (isPower) CtrlColors.Accent else CtrlColors.Surface,
                    contentColor = if (isPower) CtrlColors.OnAccent else CtrlColors.Text,
                    borderColor = if (isPower) null else CtrlColors.Outline,
                    repeat = role == KeyRole.VOL_UP || role == KeyRole.VOL_DOWN,
                ) { Icon(icon, null, Modifier.size(28.dp)) }
                Spacer(Modifier.height(6.dp))
                Text(label.uppercase(), style = CtrlType.Status, color = CtrlColors.Muted)
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
        ScreenTopBar(stringResource(R.string.find_title))
        if (index >= brands.size) {
            MessageView(
                Icons.Outlined.PowerSettingsNew,
                stringResource(R.string.find_no_match),
                stringResource(R.string.find_no_match_body),
                actionLabel = stringResource(R.string.find_start_again),
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
                SectionLabel(stringResource(R.string.find_progress, index + 1, brands.size), color = CtrlColors.Accent)
                Text(brand.brand, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.find_try),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CtrlColors.Muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                BeamKey(
                    power?.let { RemoteButton(id = "find-${brand.id}", label = stringResource(R.string.key_power), role = KeyRole.POWER, code = it) },
                    KeyRole.POWER,
                    Modifier.size(120.dp),
                    shape = CtrlShapes.Hero,
                    container = CtrlColors.Accent,
                    contentColor = CtrlColors.OnAccent,
                    borderColor = null,
                ) { Icon(Icons.Outlined.PowerSettingsNew, null, Modifier.size(48.dp)) }
                Spacer(Modifier.height(8.dp))
                PillButton(
                    stringResource(R.string.find_worked),
                    onClick = { nav.navigate(com.vknxworks.beamctrl.ui.nav.BrandRoute(brand.id)) },
                    icon = Icons.Outlined.Check,
                    modifier = Modifier.fillMaxWidth(),
                )
                CtrlSecondaryButton(onClick = { index++ }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Outlined.SkipNext, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (index == brands.lastIndex) stringResource(R.string.find_didnt_work) else stringResource(R.string.find_didnt_work_next, brands[index + 1].brand))
                }
            }
        }
    }
}
