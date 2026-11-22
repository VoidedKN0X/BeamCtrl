package com.vknxworks.beamctrl.data

import com.vknxworks.beamctrl.data.settings.AppSettings
import com.vknxworks.beamctrl.data.settings.LockTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Restoring a backup never makes BeamCtrl less private. */
class RestoreSettingsTest {
    @Test fun privacySwitchesStayOn() {
        val now = AppSettings(hideScreenshots = true, hideRecents = true)
        val file = BackupSettings.from(AppSettings(hideScreenshots = false, hideRecents = false, haptics = false))
        val after = file.applyTo(now)
        assertTrue(after.hideScreenshots && after.hideRecents)
        // Ordinary settings still come from the file.
        assertEquals(false, after.haptics)
    }

    @Test fun privacySwitchesCanBeTurnedOn() {
        val after = BackupSettings.from(AppSettings(hideScreenshots = true, hideRecents = true)).applyTo(AppSettings())
        assertTrue(after.hideScreenshots && after.hideRecents)
    }

    @Test fun lockTimeoutOnlyGetsShorter() {
        val now = AppSettings(lockTimeout = LockTimeout.MINUTES_5)
        assertEquals(LockTimeout.MINUTES_5, BackupSettings.from(AppSettings(lockTimeout = LockTimeout.RESTART)).applyTo(now).lockTimeout)
        assertEquals(LockTimeout.MINUTES_5, BackupSettings.from(AppSettings(lockTimeout = LockTimeout.HOUR_1)).applyTo(now).lockTimeout)
        assertEquals(LockTimeout.MINUTE_1, BackupSettings.from(AppSettings(lockTimeout = LockTimeout.MINUTE_1)).applyTo(now).lockTimeout)
        assertEquals(LockTimeout.IMMEDIATELY, BackupSettings.from(AppSettings(lockTimeout = LockTimeout.IMMEDIATELY)).applyTo(now).lockTimeout)
    }
}
