package com.vknxworks.beamctrl

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.beamctrl.ui.lock.AppLockGate
import com.vknxworks.beamctrl.ui.lock.lockCovering
import com.vknxworks.beamctrl.ui.nav.AppRoot
import com.vknxworks.beamctrl.ui.theme.BeamCtrlTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark only (Ctrl / Signal): edge to edge, status and navigation bars in the background colour, light icons.
        val bars = SystemBarStyle.dark(BAR_COLOR)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        val c = container
        setContent {
            val settings by c.settings.settings.collectAsStateWithLifecycle()
            // Problems sending a code show up wherever you are.
            LaunchedEffect(Unit) {
                c.blaster.errors.collect { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() }
            }
            // "Hide in screenshots": FLAG_SECURE (also blanks the Recents preview). Always on, whatever the
            // switch says, while the lock screen or a PIN screen shows.
            val secure = settings.hideScreenshots || lockCovering(c.vault)
            // "Hide in recent apps": a blank Recents card; screenshots still work. Always with the app lock on,
            // so Recents never shows what's behind the lock.
            val blankRecents = settings.hideRecents || c.vault.enabled
            LaunchedEffect(secure, blankRecents) {
                if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                setRecentsScreenshotEnabled(!blankRecents)
            }
            BeamCtrlTheme {
                // Nothing of the app is shown while the app lock is locked.
                AppLockGate { AppRoot() }
            }
        }
    }
}

/** The Ctrl / Signal background (#08080A), behind the status and navigation bars. */
private val BAR_COLOR = android.graphics.Color.rgb(8, 8, 10)
