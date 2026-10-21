package com.vknxworks.beamctrl.data.settings

import android.content.Context
import androidx.annotation.StringRes
import com.vknxworks.beamctrl.R
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "beamctrl")

enum class ThemeChoice(@StringRes val label: Int) {
    SYSTEM(R.string.theme_system),
    DARK(R.string.theme_dark),
    LIGHT(R.string.theme_light),
    BLACK(R.string.theme_black),
}

/** How long BeamCtrl can be in the background before the app lock asks again (-1: only after a restart). */
enum class LockTimeout(@StringRes val label: Int, val millis: Long) {
    IMMEDIATELY(R.string.lock_again_immediately, 0L),
    MINUTE_1(R.string.lock_again_1m, 60_000L),
    MINUTES_5(R.string.lock_again_5m, 5 * 60_000L),
    MINUTES_15(R.string.lock_again_15m, 15 * 60_000L),
    HOUR_1(R.string.lock_again_1h, 60 * 60_000L),
    RESTART(R.string.lock_again_restart, -1L),
}

data class AppSettings(
    val haptics: Boolean = true,
    /** Keep sending while a button is held (volume, channel and arrows). */
    val holdToRepeat: Boolean = true,
    val keepScreenOn: Boolean = false,
    val showNumberPad: Boolean = true,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val dynamicColor: Boolean = false,
    /** App lock: when it asks again after BeamCtrl was in the background. */
    val lockTimeout: LockTimeout = LockTimeout.IMMEDIATELY,
    /** "Hide in screenshots": FLAG_SECURE on the app's windows (screenshots, recordings and the Recents preview). */
    val hideScreenshots: Boolean = false,
    /** "Hide in recent apps": the Recents view shows a blank card. Screenshots still work. */
    val hideRecents: Boolean = false,
)

class SettingsRepository(private val context: Context, scope: CoroutineScope) {
    private object K {
        val haptics = booleanPreferencesKey("haptics")
        val repeat = booleanPreferencesKey("hold_repeat")
        val screenOn = booleanPreferencesKey("keep_screen_on")
        val numbers = booleanPreferencesKey("number_pad")
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val lockTimeout = stringPreferencesKey("lock_timeout")
        val hideScreenshots = booleanPreferencesKey("hide_screenshots")
        val hideRecents = booleanPreferencesKey("hide_recents")
        /** The earlier single "hide in screenshots and recent apps" switch; an "on" turns on both new ones. */
        val legacyHide = booleanPreferencesKey("hide_content")
    }

    init {
        // BeamCtrl 0.2.7 removed the optional anonymous stats. Delete the two settings they used (their
        // keys start with "usage_"), so nothing from them stays on the phone after the update.
        scope.launch {
            runCatching {
                context.appDataStore.edit { p ->
                    p.asMap().keys.filter { it.name.startsWith("usage_") }.forEach { p.remove(booleanPreferencesKey(it.name)) }
                }
            }
        }
    }

    val settings: StateFlow<AppSettings> = context.appDataStore.data
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    /** The stored settings as they're read and changed, without [settings]' placeholder defaults at start-up. */
    val stored: Flow<AppSettings> = context.appDataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings() = AppSettings(
        haptics = this[K.haptics] ?: true,
        holdToRepeat = this[K.repeat] ?: true,
        keepScreenOn = this[K.screenOn] ?: false,
        showNumberPad = this[K.numbers] ?: true,
        theme = this[K.theme]?.let { n -> ThemeChoice.entries.firstOrNull { it.name == n } } ?: ThemeChoice.SYSTEM,
        dynamicColor = this[K.dynamic] ?: false,
        lockTimeout = this[K.lockTimeout]?.let { n -> LockTimeout.entries.firstOrNull { it.name == n } } ?: LockTimeout.IMMEDIATELY,
        hideScreenshots = this[K.hideScreenshots] ?: this[K.legacyHide] ?: false,
        hideRecents = this[K.hideRecents] ?: this[K.legacyHide] ?: false,
    )

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.appDataStore.edit { p ->
            val s = transform(p.toSettings())
            p[K.haptics] = s.haptics
            p[K.repeat] = s.holdToRepeat
            p[K.screenOn] = s.keepScreenOn
            p[K.numbers] = s.showNumberPad
            p[K.theme] = s.theme.name
            p[K.dynamic] = s.dynamicColor
            p[K.lockTimeout] = s.lockTimeout.name
            p[K.hideScreenshots] = s.hideScreenshots
            p[K.hideRecents] = s.hideRecents
            p.remove(K.legacyHide)
        }
    }
}
