package com.vknxworks.beamctrl

import android.content.Context
import com.vknxworks.beamctrl.data.RemoteRepository
import com.vknxworks.beamctrl.data.crypto.DataKeyStore
import com.vknxworks.beamctrl.data.crypto.KeystoreWrapper
import com.vknxworks.beamctrl.data.lock.LockVault
import com.vknxworks.beamctrl.data.settings.AppSettings
import com.vknxworks.beamctrl.ui.lock.AppLock
import kotlinx.coroutines.withContext
import com.vknxworks.beamctrl.data.settings.SettingsRepository
import com.vknxworks.beamctrl.ir.Blaster
import com.vknxworks.beamctrl.ir.IrLibrary
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

/** Manual dependency container; one per process. */
class AppContainer(val context: Context) {
    /**
     * Background work that outlives a screen (saving remotes and settings). A failure there (for example a
     * full disk) must never close the app, so it's dropped here instead of crashing.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> })

    val settings = SettingsRepository(context, appScope)
    /** The key that encrypts the saved remotes at rest; itself kept by the Android Keystore. */
    private val dataKey = DataKeyStore(context.filesDir, KeystoreWrapper("beamctrl_data_at_rest"))
    val remotes = RemoteRepository(context, appScope, dataKey)
    val blaster = Blaster(context)
    val library = IrLibrary(context)

    /** The app lock's PIN and fingerprint key. BeamCtrl has no servers or passwords to seal with it. */
    val vault = LockVault(context, "beamctrl")

    /** Turns the app lock on with [pin]. */
    suspend fun enableLock(pin: String) {
        // Unlocked first, so the lock screen doesn't flash up while the PIN is being saved.
        AppLock.markUnlocked()
        withContext(Dispatchers.Default) { vault.setUp(pin) }
    }

    /** Turns the app lock off (after the PIN was confirmed). */
    fun disableLock() {
        vault.disable()
    }

    /**
     * "Forgot PIN?": the PIN can't be recovered, and skipping it would make the lock pointless, so every
     * remote and setting on the phone is deleted and the lock turned off.
     */
    suspend fun forgetEverything() {
        // After the saved remotes were read, so the load can't bring them back.
        remotes.ready.first { it }
        // Remotes that couldn't even be opened go too (the file is deleted, not set aside).
        remotes.startEmpty(keep = false)
        remotes.deleteAll()
        settings.update { AppSettings() }
        vault.disable()
        AppLock.markUnlocked()
    }

    fun onLocked() {
        vault.close()
    }

    fun onUnlocked() {
        AppLock.markUnlocked()
    }

    init {
        AppLock.onLocked = { onLocked() }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as BeamCtrlApp).container
