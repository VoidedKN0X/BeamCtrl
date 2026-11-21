package com.vknxworks.beamctrl.ui.remote

import android.view.KeyEvent

/**
 * The phone's volume keys while a remote is open (Settings > Volume keys control the remote). The open remote
 * screen sets [handler] and clears it when it closes; MainActivity offers it every volume key event first. The
 * handler returns false for a key the remote has no button for, so the phone's own volume changes as usual.
 */
object VolumeKeys {
    @Volatile
    var handler: ((KeyEvent) -> Boolean)? = null

    fun isVolumeKey(keyCode: Int): Boolean = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
}
