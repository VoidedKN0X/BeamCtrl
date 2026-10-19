package com.vknxworks.beamctrl

import android.content.Context
import com.vknxworks.beamctrl.data.RemoteRepository
import com.vknxworks.beamctrl.data.settings.SettingsRepository
import com.vknxworks.beamctrl.data.telemetry.Telemetry
import com.vknxworks.beamctrl.ir.Blaster
import com.vknxworks.beamctrl.ir.IrLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container; one per process. */
class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsRepository(context, appScope)
    val remotes = RemoteRepository(context, appScope)
    val blaster = Blaster(context)
    val library = IrLibrary(context)
    val telemetry = Telemetry(context, settings, appScope, hasIr = blaster.available)
}

val Context.container: AppContainer
    get() = (applicationContext as BeamCtrlApp).container
