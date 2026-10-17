package com.vknxworks.beamctrl.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "beamctrl")

enum class ThemeChoice(val label: String) { SYSTEM("Follow system"), DARK("Dark"), LIGHT("Light"), BLACK("Pure black") }

data class AppSettings(
    val haptics: Boolean = true,
    /** Keep sending while a button is held (volume, channel and arrows). */
    val holdToRepeat: Boolean = true,
    val keepScreenOn: Boolean = false,
    val showNumberPad: Boolean = true,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val dynamicColor: Boolean = false,
)

class SettingsRepository(private val context: Context, scope: CoroutineScope) {
    private object K {
        val haptics = booleanPreferencesKey("haptics")
        val repeat = booleanPreferencesKey("hold_repeat")
        val screenOn = booleanPreferencesKey("keep_screen_on")
        val numbers = booleanPreferencesKey("number_pad")
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic_color")
    }

    val settings: StateFlow<AppSettings> = context.appDataStore.data
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    private fun Preferences.toSettings() = AppSettings(
        haptics = this[K.haptics] ?: true,
        holdToRepeat = this[K.repeat] ?: true,
        keepScreenOn = this[K.screenOn] ?: false,
        showNumberPad = this[K.numbers] ?: true,
        theme = this[K.theme]?.let { n -> ThemeChoice.entries.firstOrNull { it.name == n } } ?: ThemeChoice.SYSTEM,
        dynamicColor = this[K.dynamic] ?: false,
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
        }
    }
}
