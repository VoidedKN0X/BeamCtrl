package com.vknxworks.beamctrl.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.ir.IrLibrary
import com.vknxworks.beamctrl.ir.LibraryDevice
import com.vknxworks.beamctrl.ui.components.MessageView
import com.vknxworks.beamctrl.ui.components.ScreenTopBar
import com.vknxworks.beamctrl.ui.nav.LibraryBrandRoute
import com.vknxworks.beamctrl.ui.nav.LibraryCategoryRoute
import com.vknxworks.beamctrl.ui.nav.LibraryDeviceRoute
import com.vknxworks.beamctrl.ui.nav.LocalNavigator
import com.vknxworks.beamctrl.ui.theme.Gutter
import kotlinx.coroutines.delay
import java.text.NumberFormat

/** Loads something from the library, keyed so it reloads when [key] changes. Null while loading. */
@Composable
private fun <T> rememberLibrary(key: Any?, block: suspend IrLibrary.() -> T): T? {
    val library = LocalContext.current.container.library
    val state by produceState<T?>(null, key) { value = library.block() }
    return state
}

private fun count(n: Int): String = NumberFormat.getIntegerInstance().format(n)

/** Code library home: search across every remote, or browse by device type. */
@Composable
fun LibraryScreen(initialQuery: String) {
    val nav = LocalNavigator.current
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    // Wait for a pause in typing before searching 8,000+ remotes.
    var debounced by remember { mutableStateOf(initialQuery) }
    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(200)
        debounced = query
    }
    val index = rememberLibrary(Unit) { index() }
    val results = rememberLibrary(debounced) { if (debounced.isBlank()) emptyList() else search(debounced) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Code library")
        SearchField(query, onChange = { query = it }, placeholder = "Brand or model, e.g. LG soundbar")
        when {
            index == null -> Loading()
            query.isNotBlank() -> when {
                results == null || debounced != query -> Loading()
                results.isEmpty() -> MessageView(Icons.Outlined.SearchOff, "No remotes found", "Try just the brand, or fewer words.")
                else -> LazyColumn(contentPadding = listPadding()) {
                    cardList(results, key = { it.id }) { device, _ ->
                        val cat = index.categories.getOrNull(device.category)
                        ListRow(
                            title = device.title,
                            subtitle = listOfNotNull(cat?.name, "${device.signalCount} buttons").joinToString("  ·  "),
                            icon = cat?.type?.icon,
                            power = device.hasPower,
                            onClick = { nav.navigate(LibraryDeviceRoute(device.id)) },
                        )
                    }
                }
            }
            else -> LazyColumn(contentPadding = listPadding()) {
                item {
                    Text(
                        "${count(index.devices.size)} remotes from the Flipper-IRDB community database, built into the app. " +
                            "Pick one, try its buttons, and save it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Gutter, end = Gutter, top = 4.dp, bottom = 16.dp),
                    )
                }
                cardList(index.categories.filter { it.deviceCount > 0 }, key = { it.index }) { cat, _ ->
                    ListRow(
                        title = cat.name,
                        subtitle = "${count(cat.deviceCount)} remotes",
                        icon = cat.type.icon,
                        onClick = { nav.navigate(LibraryCategoryRoute(cat.index)) },
                    )
                }
            }
        }
    }
}

/** Brands in one category, with a filter. A brand with a single remote opens it straight away. */
@Composable
fun LibraryCategoryScreen(categoryIndex: Int) {
    val nav = LocalNavigator.current
    val category = rememberLibrary(categoryIndex) { category(categoryIndex) }
    val brands = rememberLibrary(categoryIndex) { brands(categoryIndex) }
    var filter by rememberSaveable { mutableStateOf("") }
    val library = LocalContext.current.container.library

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(category?.name ?: "Code library")
        SearchField(filter, onChange = { filter = it }, placeholder = "Filter brands")
        val shown = brands?.filter { it.first.contains(filter.trim(), ignoreCase = true) }
        when {
            shown == null -> Loading()
            shown.isEmpty() -> MessageView(Icons.Outlined.SearchOff, "No brands match", "Check the spelling, or search the whole library instead.")
            else -> {
                var opening by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(opening) {
                    val brand = opening ?: return@LaunchedEffect
                    val devices = library.devices(categoryIndex, brand)
                    if (devices.size == 1) nav.navigate(LibraryDeviceRoute(devices.first().id))
                    else nav.navigate(LibraryBrandRoute(categoryIndex, brand))
                    opening = null
                }
                LazyColumn(contentPadding = listPadding()) {
                    cardList(shown, key = { it.first }) { (brand, n), _ ->
                        ListRow(
                            title = brand,
                            subtitle = if (n == 1) "1 remote" else "$n remotes",
                            onClick = { opening = brand },
                        )
                    }
                }
            }
        }
    }
}

/** Every remote for one brand in one category. */
@Composable
fun LibraryBrandScreen(categoryIndex: Int, brand: String) {
    val nav = LocalNavigator.current
    val devices = rememberLibrary(categoryIndex to brand) { devices(categoryIndex, brand) }
    val category = rememberLibrary(categoryIndex) { category(categoryIndex) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(listOfNotNull(brand, category?.name).joinToString(" "))
        when {
            devices == null -> Loading()
            devices.isEmpty() -> MessageView(Icons.Filled.LibraryBooks, "No remotes here")
            else -> LazyColumn(contentPadding = listPadding(top = 8.dp)) {
                item {
                    Text(
                        "Not sure which one is yours? Most models from the same brand share codes, so pick the closest and try its buttons.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Gutter, end = Gutter, bottom = 16.dp),
                    )
                }
                cardList(devices, key = { it.id }) { device, _ ->
                    ListRow(
                        title = device.model,
                        subtitle = "${device.signalCount} buttons",
                        power = device.hasPower,
                        onClick = { nav.navigate(LibraryDeviceRoute(device.id)) },
                    )
                }
            }
        }
    }
}

// ---------- Pieces ----------

private fun listPadding(top: androidx.compose.ui.unit.Dp = 4.dp) = PaddingValues(top = top, bottom = 32.dp)

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
internal fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String) {
    val focus = LocalFocusManager.current
    TextField(
        value = value,
        onValueChange = { onChange(it.take(60)) },
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Close, "Clear") }
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp),
    )
}

/**
 * Items drawn as one rounded card, like the grouped lists elsewhere in the app, but lazily, so
 * lists of thousands of remotes scroll smoothly.
 */
private fun <T> LazyListScope.cardList(
    items: List<T>,
    key: (T) -> Any,
    content: @Composable (T, Int) -> Unit,
) {
    itemsIndexed(items, key = { _, item -> key(item) }) { i, item ->
        val big = 24.dp
        val shape = RoundedCornerShape(
            topStart = if (i == 0) big else 0.dp,
            topEnd = if (i == 0) big else 0.dp,
            bottomStart = if (i == items.lastIndex) big else 0.dp,
            bottomEnd = if (i == items.lastIndex) big else 0.dp,
        )
        Box(
            Modifier
                .padding(horizontal = Gutter)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(top = if (i == 0) 6.dp else 0.dp, bottom = if (i == items.lastIndex) 6.dp else 0.dp),
        ) { content(item, i) }
    }
}

@Composable
private fun ListRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    power: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (power) {
            Icon(
                Icons.Filled.PowerSettingsNew,
                "Has a power button",
                Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** For the Add tab: total remotes in the library, null while it loads. */
@Composable
fun rememberLibrarySize(): Int? = rememberLibrary(Unit) { index().devices.size }
