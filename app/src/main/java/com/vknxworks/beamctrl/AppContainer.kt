package com.vknxworks.beamctrl

import android.content.Context
import com.vknxworks.beamctrl.data.ExtrasRepository
import com.vknxworks.beamctrl.data.RemoteRepository
import com.vknxworks.beamctrl.ir.MacroRunner
import com.vknxworks.beamctrl.ir.SleepAlarm
import com.vknxworks.beamctrl.ir.SleepPlan
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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
    /** Macros and the sleep timer, encrypted at rest with the same key as the remotes. */
    val extras = ExtrasRepository(context.filesDir, appScope, dataKey)
    val blaster = Blaster(context)
    val library = IrLibrary(context)
    /** Runs macros (one at a time). */
    val macroRunner = MacroRunner(appScope, remotes, blaster)

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
        extras.ready.first { it }
        extras.startEmpty(keep = false)
        extras.deleteAll()
        SleepAlarm.cancel(context)
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
        // A deleted remote or button takes its macro steps and sleep timer with it. Only once both files have been
        // read and opened, so an empty list while loading (or remotes that can't be opened) never wipes macros.
        appScope.launch {
            combine(remotes.remotes, remotes.ready, remotes.unreadable, extras.ready, extras.unreadable) { list, rReady, rBad, eReady, eBad ->
                if (rReady && !rBad && eReady && !eBad) list else null
            }.collect { list -> if (list != null) extras.dropMissing(list) }
        }
        // Android drops alarms on a reboot, a force stop and an app update, but the timer stays saved (and shown).
        // Set the alarm again once the saved extras are read; a timer that is long past is cleared instead.
        appScope.launch {
            extras.ready.first { it }
            if (extras.unreadable.value) return@launch
            val timer = extras.extras.value.sleep ?: return@launch
            val at = SleepPlan.alarmAtStart(timer.at, System.currentTimeMillis())
            if (at != null) SleepAlarm.schedule(context, at) else extras.setSleep(null)
        }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as BeamCtrlApp).container
