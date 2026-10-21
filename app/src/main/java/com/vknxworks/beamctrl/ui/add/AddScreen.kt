package com.vknxworks.beamctrl.ui.add

import android.net.Uri
import com.vknxworks.beamctrl.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.MaterialTheme
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
import com.vknxworks.beamctrl.data.FileTooBigException
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.data.readTextCapped
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
import com.vknxworks.beamctrl.ui.nav.LibraryRoute
import com.vknxworks.beamctrl.ui.nav.PowerOffRoute
import com.vknxworks.beamctrl.ui.library.rememberLibrarySize
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
                    val text = context.contentResolver.openInputStream(uri)?.use { it.readTextCapped() }.orEmpty()
                    val name = displayName(context, uri)?.substringBeforeLast('.')?.replace('_', ' ')?.trim()
                    name to FlipperIr.parse(text, context.getString(R.string.button_default))
                }
            }
            result.onSuccess { (name, parsed) ->
                if (parsed.buttons.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.import_no_signals), Toast.LENGTH_LONG).show()
                    return@onSuccess
                }
                val remote = c.remotes.add(
                    Remote(name = name?.ifBlank { null } ?: context.getString(R.string.import_default_name), type = DeviceType.OTHER, buttons = parsed.buttons),
                )
                val skipped = parsed.skipped.size
                val imported = context.resources.getQuantityString(R.plurals.import_done, parsed.buttons.size, parsed.buttons.size)
                Toast.makeText(
                    context,
                    if (skipped > 0) context.resources.getQuantityString(R.plurals.import_done_skipped, skipped, imported, skipped) else imported,
                    Toast.LENGTH_LONG,
                ).show()
                nav.openRemote(remote.id, fromAdd = true)
            }.onFailure {
                val message = if (it is FileTooBigException) R.string.import_too_big else R.string.import_unreadable
                Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
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
        TabTitle(stringResource(R.string.add_title))

        val librarySize = rememberLibrarySize()
        Group(
            stringResource(R.string.library_title),
            footer = stringResource(R.string.add_library_footer),
        ) {
            GroupRow(
                stringResource(R.string.add_library_search),
                librarySize?.let {
                    pluralStringResource(R.plurals.add_library_size, it, java.text.NumberFormat.getIntegerInstance().format(it))
                } ?: stringResource(R.string.loading),
                icon = Icons.Filled.LibraryBooks,
                showChevron = true,
                onClick = { nav.navigate(LibraryRoute()) },
            )
        }

        Group(footer = stringResource(R.string.add_find_footer)) {
            GroupRow(
                stringResource(R.string.find_title),
                stringResource(R.string.add_find_subtitle),
                icon = Icons.Filled.Search,
                showChevron = true,
                onClick = { nav.navigate(FindBrandRoute) },
            )
            GroupRow(
                stringResource(R.string.poweroff_title),
                stringResource(R.string.add_poweroff_subtitle),
                icon = Icons.Filled.PowerSettingsNew,
                iconTint = MaterialTheme.colorScheme.error,
                showChevron = true,
                onClick = { nav.navigate(PowerOffRoute) },
            )
        }

        Group(stringResource(R.string.add_brands)) {
            CodeLibrary.brands.forEach { brand ->
                GroupRow(
                    brand.title(context),
                    stringResource(
                        R.string.dot_pair,
                        pluralStringResource(R.plurals.buttons_count, brand.keys.size, brand.keys.size),
                        brand.keys.first().second.protocolLabel(context),
                    ),
                    icon = Icons.Filled.Tv,
                    showChevron = true,
                    onClick = { nav.navigate(BrandRoute(brand.id)) },
                )
            }
        }

        Group(
            stringResource(R.string.add_build),
            footer = stringResource(R.string.add_build_footer),
        ) {
            GroupRow(
                stringResource(R.string.add_import),
                stringResource(R.string.add_import_subtitle),
                icon = Icons.Filled.FileUpload,
                showChevron = true,
                onClick = { importIr.launch(arrayOf("*/*")) },
            )
            GroupRow(
                stringResource(R.string.add_blank),
                stringResource(R.string.add_blank_subtitle),
                icon = Icons.Filled.Edit,
                showChevron = true,
                onClick = { naming = true },
            )
        }
    }

    if (naming) {
        NameDialog(stringResource(R.string.new_remote), stringResource(R.string.new_remote_default), confirm = stringResource(R.string.create), onDismiss = { naming = false }) { name ->
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
