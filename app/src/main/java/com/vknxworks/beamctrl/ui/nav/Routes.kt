package com.vknxworks.beamctrl.ui.nav

import kotlinx.serialization.Serializable

// Tabs
@Serializable data object RemotesRoute
@Serializable data object AddRoute
@Serializable data object SettingsRoute

@Serializable data class RemoteRoute(val id: String, val edit: Boolean = false)
@Serializable data class BrandRoute(val id: String)
@Serializable data object FindBrandRoute
