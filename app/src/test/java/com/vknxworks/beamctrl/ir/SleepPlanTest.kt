package com.vknxworks.beamctrl.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepPlanTest {
    private val now = 1_000_000_000L

    @Test fun futureTimerIsSetAgainForItsTime() {
        assertEquals(now + 30 * 60_000L, SleepPlan.alarmAtStart(now + 30 * 60_000L, now))
        assertEquals(now, SleepPlan.alarmAtStart(now, now))
    }

    @Test fun justMissedTimerGoesOffNow() {
        assertEquals(now, SleepPlan.alarmAtStart(now - 60_000L, now))
        assertEquals(now, SleepPlan.alarmAtStart(now - SleepPlan.LATE_GRACE_MS, now))
    }

    @Test fun longPastTimerIsCleared() {
        assertNull(SleepPlan.alarmAtStart(now - SleepPlan.LATE_GRACE_MS - 1, now))
        assertNull(SleepPlan.alarmAtStart(now - 8 * 3_600_000L, now))
    }
}
