package com.vknxworks.beamctrl.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.ui.add.AddScreen
import com.vknxworks.beamctrl.ui.add.BrandScreen
import com.vknxworks.beamctrl.ui.add.FindBrandScreen
import com.vknxworks.beamctrl.ui.library.LibraryBrandScreen
import com.vknxworks.beamctrl.ui.library.LibraryCategoryScreen
import com.vknxworks.beamctrl.ui.library.LibraryDeviceScreen
import com.vknxworks.beamctrl.ui.library.LibraryScreen
import com.vknxworks.beamctrl.ui.poweroff.PowerOffScreen
import com.vknxworks.beamctrl.ui.components.LocalBottomClearance
import com.vknxworks.beamctrl.ui.components.LocalHazeState
import com.vknxworks.beamctrl.ui.remote.RemoteScreen
import com.vknxworks.beamctrl.ui.remotes.RemotesScreen
import com.vknxworks.beamctrl.ui.settings.SettingsScreen
import com.vknxworks.beamctrl.ui.settings.YouScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun AppRoot() {
    val c = LocalContext.current.container
    val ready by c.remotes.ready.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            MainScaffold()
        }
    }
}

private data class TopDest(val route: Any, val item: TabItem)

/** Only content tabs: Settings, Backup and About open from the profile picture. */
private val topDestinations = listOf(
    TopDest(RemotesRoute, TabItem("Remotes", Icons.Outlined.SettingsRemote, Icons.Filled.SettingsRemote)),
    TopDest(AddRoute, TabItem("Add", Icons.Outlined.AddCircleOutline, Icons.Filled.AddCircle)),
)

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val navigator = remember(navController) { Navigator(navController) }
    val backStack by navController.currentBackStackEntryAsState()
    val dest = backStack?.destination
    val selectedIndex = topDestinations.indexOfFirst { top -> dest?.hierarchy?.any { it.hasRoute(top.route::class) } == true }

    fun selectTab(i: Int) {
        navController.navigate(topDestinations[i].route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (maxWidth >= 600.dp) {
                // Tablets and unfolded foldables: a side rail.
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding(),
                    ) {
                        Spacer(Modifier.weight(1f))
                        topDestinations.forEachIndexed { i, top ->
                            val selected = i == selectedIndex
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectTab(i) },
                                icon = { Icon(if (selected) top.item.selectedIcon else top.item.icon, contentDescription = top.item.label) },
                                label = { Text(top.item.label) },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    Box(Modifier.weight(1f).fillMaxHeight().navigationBarsPadding()) { AppNavHost(navController) }
                }
            } else {
                PhoneLayout(
                    navController = navController,
                    showTabs = selectedIndex >= 0,
                    selectedIndex = selectedIndex,
                    onSelect = ::selectTab,
                )
            }
        }
    }
}

/**
 * Phones: content runs edge to edge and scrolls under a floating glass island.
 * The island shows on the main tabs; remotes and other pushed screens get the full height.
 */
@Composable
private fun PhoneLayout(
    navController: NavHostController,
    showTabs: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    var overlayHeight by remember { mutableStateOf(0.dp) }

    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalBottomClearance provides if (showTabs) overlayHeight else 0.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
                AppNavHost(navController)
            }
            AnimatedVisibility(
                visible = showTabs,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { overlayHeight = with(density) { it.height.toDp() } },
            ) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                    GlassTabBar(
                        items = topDestinations.map { it.item },
                        selectedIndex = selectedIndex,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = RemotesRoute) {
        composable<RemotesRoute> { RemotesScreen() }
        composable<AddRoute> { AddScreen() }
        composable<YouRoute> { YouScreen() }
        composable<SettingsRoute> { SettingsScreen() }

        composable<RemoteRoute> {
            val r = it.toRoute<RemoteRoute>()
            RemoteScreen(r.id, r.edit)
        }
        composable<BrandRoute> { BrandScreen(it.toRoute<BrandRoute>().id) }
        composable<FindBrandRoute> { FindBrandScreen() }

        composable<LibraryRoute> { LibraryScreen(it.toRoute<LibraryRoute>().query) }
        composable<LibraryCategoryRoute> { LibraryCategoryScreen(it.toRoute<LibraryCategoryRoute>().category) }
        composable<LibraryBrandRoute> {
            val r = it.toRoute<LibraryBrandRoute>()
            LibraryBrandScreen(r.category, r.brand)
        }
        composable<LibraryDeviceRoute> { LibraryDeviceScreen(it.toRoute<LibraryDeviceRoute>().id) }
        composable<PowerOffRoute> { PowerOffScreen() }
    }
}
