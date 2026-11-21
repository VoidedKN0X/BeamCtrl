package com.vknxworks.beamctrl.ir

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.SleepTimer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The sleep timer's alarm. Android wakes [SleepReceiver] at the chosen time, even with the screen off and BeamCtrl
 * closed, and it presses the chosen button once. With "Alarms and reminders" allowed for BeamCtrl the alarm is exact;
 * without it Android may run it up to 10 minutes late.
 */
object SleepAlarm {
    /** How late Android may be when exact alarms aren't allowed. */
    const val WINDOW_MS = 10 * 60_000L

    private fun manager(context: Context): AlarmManager? = context.getSystemService(AlarmManager::class.java)

    /** Whether "Alarms and reminders" is allowed, so the timer goes off on time. */
    fun exact(context: Context): Boolean = runCatching { manager(context)?.canScheduleExactAlarms() == true }.getOrDefault(false)

    fun schedule(context: Context, at: Long) {
        val am = manager(context) ?: return
        val pi = pending(context)
        try {
            if (exact(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, pi)
        } catch (e: SecurityException) {
            // Exact alarms were taken away between the check and the call: fall back to the window.
            am.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, pi)
        }
    }

    fun cancel(context: Context) {
        manager(context)?.cancel(pending(context))
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context.applicationContext,
        0,
        Intent(context.applicationContext, SleepReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Not exported: only BeamCtrl's own alarm reaches it. */
class SleepReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val c = context.container
        val done = goAsync()
        c.appScope.launch {
            try {
                withTimeoutOrNull(8_000) {
                    c.remotes.ready.first { it }
                    c.extras.ready.first { it }
                    val timer: SleepTimer = c.extras.extras.value.sleep ?: return@withTimeoutOrNull
                    // Set again for later in the meantime: wait for that one.
                    if (timer.at > System.currentTimeMillis() + 60_000) {
                        SleepAlarm.schedule(context, timer.at)
                        return@withTimeoutOrNull
                    }
                    c.extras.setSleep(null)
                    val button = c.remotes.get(timer.remoteId)?.buttons?.firstOrNull { it.id == timer.buttonId }
                    if (button != null) c.blaster.sendAndWait(button.code)
                }
            } finally {
                done.finish()
            }
        }
    }
}
