package com.vknxworks.beamctrl.ir

/** What to do with a saved sleep timer when BeamCtrl starts. Plain Kotlin, unit-tested. */
object SleepPlan {
    /**
     * A timer that was due less than this long ago still goes off (BeamCtrl was restarted around its time, for
     * example by an update). Older ones are cleared: pressing the button hours later would surprise you.
     */
    const val LATE_GRACE_MS = 5 * 60_000L

    /**
     * Android forgets alarms on a reboot, a force stop or an app update, while the timer stays saved. At start-up
     * the alarm is set again: returns the time to set it for, or null when the timer is too old and should be cleared.
     */
    fun alarmAtStart(at: Long, now: Long): Long? = when {
        at >= now -> at
        now - at <= LATE_GRACE_MS -> now
        else -> null
    }
}
