package com.vknxworks.beamctrl.ui.add

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.DeviceType
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.ir.CodeLibrary
import com.vknxworks.beamctrl.ir.FlipperIr
import com.vknxworks.beamctrl.ir.protocolLabel
import com.vknxworks.beamctrl.ui.components.Group
import com.vknxworks.beamctrl.ui.components.GroupRow
import com.vknxworks.beamctrl.ui.components.LocalBottomClearance
import com.vknxworks.beamctrl.ui.components.NameDialog
import com.vknxworks.beamctrl.ui.components.TabTitle
import com.vknxworks.beamctrl.ui.nav.BrandRoute
import com.vknxworks.beamctrl.ui.nav.FindBrandRoute
import com.vknxworks.beamctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The Add tab: ready-made TV remotes, a brand finder, Flipper imports and blank remotes. */
@Composable
fun AddScreen() {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var naming by remember { mutableStateOf(false) }

    val importIr = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }.orEmpty()
                    val name = displayName(context, uri)?.substringBeforeLast('.')?.replace('_', ' ')?.trim()
                    name to FlipperIr.parse(text)
                }
            }
            result.onSuccess { (name, parsed) ->
                if (parsed.buttons.isEmpty()) {
                    Toast.makeText(context, "No signals this app can send were found in that file", Toast.LENGTH_LONG).show()
                    return@onSuccess
                }
                val remote = c.remotes.add(
                    Remote(name = name?.ifBlank { null } ?: "Imported remote", type = DeviceType.OTHER, buttons = parsed.buttons),
                )
                val skipped = parsed.skipped.size
                Toast.makeText(
                    context,
                    "Imported ${parsed.buttons.size} buttons" + if (skipped > 0) " ($skipped in unsupported formats skipped)" else "",
                    Toast.LENGTH_LONG,
                ).show()
                nav.openRemote(remote.id, fromAdd = true)
            }.onFailure {
                Toast.makeText(context, "Couldn't read that file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 24.dp + LocalBottomClearance.current),
    ) {
        TabTitle("Add a remote")

        Group(footer = "Point your phone at the TV and try each brand's power code until one works.") {
            GroupRow(
                "Find my TV",
                "Not sure which codes your TV uses? Test them here",
                icon = Icons.Filled.Search,
                showChevron = true,
                onClick = { nav.navigate(FindBrandRoute) },
            )
        }

        Group("TV brands") {
            CodeLibrary.brands.forEach { brand ->
                GroupRow(
                    brand.title,
                    "${brand.keys.size} buttons  ·  ${brand.keys.first().second.protocolLabel}",
                    icon = Icons.Filled.Tv,
                    showChevron = true,
                    onClick = { nav.navigate(BrandRoute(brand.id)) },
                )
            }
        }

        Group(
            "Build your own",
            footer = "Phones can send infrared but not learn it from another remote, so codes come from the brand list, " +
                "a Flipper Zero .ir file (for example from the Flipper-IRDB collection), or typing them in: NEC, Samsung, " +
                "Sony, RC5, RC6, Pronto hex or raw timings.",
        ) {
            GroupRow(
                "Import a Flipper .ir file",
                "Turns every signal in the file into a button",
                icon = Icons.Filled.FileUpload,
                showChevron = true,
                onClick = { importIr.launch(arrayOf("*/*")) },
            )
            GroupRow(
                "Blank remote",
                "Start empty and add buttons one by one",
                icon = Icons.Filled.Edit,
                showChevron = true,
                onClick = { naming = true },
            )
        }
    }

    if (naming) {
        NameDialog("New remote", "My remote", confirm = "Create", onDismiss = { naming = false }) { name ->
            naming = false
            val remote = c.remotes.add(Remote(name = name, type = DeviceType.OTHER))
            nav.openRemote(remote.id, edit = true, fromAdd = true)
        }
    }
}

private fun displayName(context: android.content.Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()
