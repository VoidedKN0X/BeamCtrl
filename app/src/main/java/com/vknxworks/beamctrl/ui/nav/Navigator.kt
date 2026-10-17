package com.vknxworks.beamctrl.ui.nav

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

/** One place that knows where things open. */
class Navigator(val nav: NavHostController) {

    fun navigate(route: Any) = nav.navigate(route) { launchSingleTop = true }

    fun back() {
        nav.popBackStack()
    }

    /** Opens a remote; after adding one, the add screens are taken off the back stack. */
    fun openRemote(id: String, edit: Boolean = false, fromAdd: Boolean = false) {
        nav.navigate(RemoteRoute(id, edit)) {
            launchSingleTop = true
            if (fromAdd) popUpTo(RemotesRoute) { inclusive = false }
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator") }
