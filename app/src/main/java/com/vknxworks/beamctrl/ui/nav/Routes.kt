package com.vknxworks.beamctrl.ui.nav

import kotlinx.serialization.Serializable

// Tabs
@Serializable data object RemotesRoute
@Serializable data object AddRoute

// Opened from the profile picture: the You page, and Settings from there
@Serializable data object YouRoute
@Serializable data object SettingsRoute

@Serializable data class RemoteRoute(val id: String, val edit: Boolean = false)
@Serializable data class BrandRoute(val id: String)
@Serializable data object FindBrandRoute

// Code library
@Serializable data class LibraryRoute(val query: String = "")
@Serializable data class LibraryCategoryRoute(val category: Int)
@Serializable data class LibraryBrandRoute(val category: Int, val brand: String)
@Serializable data class LibraryDeviceRoute(val id: Int)
@Serializable data object PowerOffRoute
