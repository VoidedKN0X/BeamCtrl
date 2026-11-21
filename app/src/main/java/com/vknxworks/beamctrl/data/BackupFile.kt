package com.vknxworks.beamctrl.data

import androidx.annotation.StringRes
import com.vknxworks.beamctrl.R
import com.vknxworks.beamctrl.data.settings.AppSettings
import com.vknxworks.beamctrl.data.settings.LockTimeout
import com.vknxworks.beamctrl.data.settings.ThemeChoice
import com.vknxworks.beamctrl.ir.encode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * A BeamCtrl backup file ("Back up to a file" on the You page): every remote and every setting, so
 * the file can be restored on this phone or another one. The app lock's PIN and fingerprint keys are
 * never in it (they belong to this phone's secure hardware), and neither is whether the lock is on.
 *
 * Version 1 files ("Export all remotes" before 0.4.0) only hold remotes; they still restore. Version 3 (1.0) adds
 * macros, rooms, layouts, air-conditioner states and the newer protocols; older BeamCtrl versions refuse it as
 * "made by a newer BeamCtrl" instead of reading half of it.
 */
@Serializable
data class Backup(
    val app: String,
    val version: Int,
    /** When the backup was made (milliseconds since 1970). Missing in version 1 files. */
    val created: Long? = null,
    val remotes: List<Remote>,
    /** Missing in version 1 files, which only held remotes. */
    val settings: BackupSettings? = null,
    /** Missing before version 3. */
    val macros: List<Macro>? = null,
)

/** Every setting in [AppSettings]. Kept separate so the file format doesn't change by accident. */
@Serializable
data class BackupSettings(
    val theme: ThemeChoice,
    val dynamicColor: Boolean,
    val haptics: Boolean,
    val holdToRepeat: Boolean,
    val showNumberPad: Boolean,
    val keepScreenOn: Boolean,
    val lockTimeout: LockTimeout,
    val hideScreenshots: Boolean,
    val hideRecents: Boolean,
    /** Missing before version 3: on, as on a new install. */
    val volumeKeys: Boolean = true,
) {
    fun applyTo(s: AppSettings) = s.copy(
        theme = theme,
        dynamicColor = dynamicColor,
        haptics = haptics,
        holdToRepeat = holdToRepeat,
        showNumberPad = showNumberPad,
        keepScreenOn = keepScreenOn,
        lockTimeout = lockTimeout,
        hideScreenshots = hideScreenshots,
        hideRecents = hideRecents,
        volumeKeys = volumeKeys,
    )

    companion object {
        fun from(s: AppSettings) = BackupSettings(
            theme = s.theme,
            dynamicColor = s.dynamicColor,
            haptics = s.haptics,
            holdToRepeat = s.holdToRepeat,
            showNumberPad = s.showNumberPad,
            keepScreenOn = s.keepScreenOn,
            lockTimeout = s.lockTimeout,
            hideScreenshots = s.hideScreenshots,
            hideRecents = s.hideRecents,
            volumeKeys = s.volumeKeys,
        )
    }
}

/** Why a file can't be restored; [reason] is shown as it is. */
class BackupException(@StringRes val reason: Int) : Exception()

object BackupFile {
    const val APP = "BeamCtrl"
    const val VERSION = 3

    const val MAX_REMOTES = 500
    const val MAX_BUTTONS = 500
    const val MAX_NAME = 80
    const val MAX_LABEL = 60

    /** Strict: unknown fields, wrong types and unknown values make the whole file fail. */
    private val strict = Json {
        ignoreUnknownKeys = false
        isLenient = false
        coerceInputValues = false
        encodeDefaults = true
    }

    fun write(remotes: List<Remote>, settings: AppSettings, macros: List<Macro>, now: Long = System.currentTimeMillis()): String =
        strict.encodeToString(
            Backup.serializer(),
            Backup(app = APP, version = VERSION, created = now, remotes = remotes, settings = BackupSettings.from(settings), macros = macros),
        )

    /**
     * Reads and checks a whole backup file before anything on the phone is touched. Anything malformed is
     * refused with a [BackupException] instead of being skipped or repaired, so a restore either brings
     * back exactly what was backed up or changes nothing.
     */
    fun read(text: String): Backup {
        // First the two fields that say what the file is, so the message can say so.
        val root = runCatching { strict.parseToJsonElement(text) }.getOrNull() as? JsonObject
            ?: throw BackupException(R.string.backup_error_not_beamctrl)
        val app = (root["app"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (app != APP) throw BackupException(R.string.backup_error_not_beamctrl)
        val version = (root["version"] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
            ?: throw BackupException(R.string.backup_error_damaged)
        if (version > VERSION) throw BackupException(R.string.backup_error_newer)
        if (version < 1) throw BackupException(R.string.backup_error_damaged)

        val backup = runCatching { strict.decodeFromJsonElement(Backup.serializer(), root) }
            .getOrElse { throw BackupException(R.string.backup_error_damaged) }
        if (!backup.isValid()) throw BackupException(R.string.backup_error_damaged)
        return backup
    }

    private fun Backup.isValid(): Boolean {
        // Version 2 always has the settings and a date; version 1 never had settings.
        if (version >= 2 && (settings == null || created == null)) return false
        if (version == 1 && settings != null) return false
        if (version < 3 && macros != null) return false
        if (created != null && created < 0) return false
        if (remotes.size > MAX_REMOTES) return false
        val remoteIds = HashSet<String>()
        for (r in remotes) {
            if (!r.id.isSaneId() || !remoteIds.add(r.id)) return false
            if (!r.name.isCleanText(MAX_NAME) || r.name.isBlank()) return false
            if (r.brand != null && (!r.brand.isCleanText(MAX_NAME) || r.brand.isBlank())) return false
            if (r.created < 0 || r.lastUsed < 0) return false
            if (r.room != null && (!r.room.isCleanText(MAX_NAME) || r.room.isBlank())) return false
            if (r.gridColumns !in 2..4) return false
            if (r.acState?.isValid() == false) return false
            if (r.buttons.size > MAX_BUTTONS) return false
            val buttonIds = HashSet<String>()
            for (b in r.buttons) {
                if (!b.id.isSaneId() || !buttonIds.add(b.id)) return false
                if (!b.label.isCleanText(MAX_LABEL)) return false
                // Only codes the blaster can actually send.
                if (runCatching { b.code.encode() }.isFailure) return false
                if (b.ac?.isValid() == false) return false
            }
        }
        val all = macros.orEmpty()
        if (all.size > MacroLimits.MAX_MACROS) return false
        val buttons = remotes.associate { r -> r.id to r.buttons.map { it.id }.toSet() }
        val macroIds = HashSet<String>()
        for (m in all) {
            if (!m.id.isSaneId() || !macroIds.add(m.id)) return false
            if (!m.name.isCleanText(MAX_NAME) || m.name.isBlank()) return false
            if (m.created < 0 || m.steps.size > MacroLimits.MAX_STEPS) return false
            // Every step must press a button that is in this same file.
            if (m.steps.any { s -> s.delayMs !in 0..MacroLimits.MAX_DELAY_MS || buttons[s.remoteId]?.contains(s.buttonId) != true }) return false
        }
        return true
    }

    private fun AcState.isValid(): Boolean = temperature == null || temperature in AcState.MIN_TEMP..AcState.MAX_TEMP

    fun String.isSaneId(): Boolean = length in 1..64 && all { it.isLetterOrDigit() || it == '-' }

    private fun String.isCleanText(max: Int): Boolean = length <= max && none { it.isISOControl() }
}
