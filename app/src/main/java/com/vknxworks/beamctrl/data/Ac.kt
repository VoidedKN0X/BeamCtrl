package com.vknxworks.beamctrl.data

import androidx.annotation.StringRes
import com.vknxworks.beamctrl.R
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class AcMode(@StringRes val label: Int) {
    OFF(R.string.ac_off),
    COOL(R.string.ac_cool),
    HEAT(R.string.ac_heat),
    DRY(R.string.ac_dry),
    FAN(R.string.ac_fan_only),
    AUTO(R.string.ac_auto),
}

@Serializable
enum class AcFan(@StringRes val label: Int) {
    AUTO(R.string.ac_fan_auto),
    LOW(R.string.ac_fan_low),
    MEDIUM(R.string.ac_fan_medium),
    HIGH(R.string.ac_fan_high),
}

/**
 * The state an air conditioner is in after a button: air-conditioner remotes send the whole state with every
 * press (mode, temperature, fan), so each saved code stands for one state. BeamCtrl remembers the last one it sent.
 */
@Serializable
data class AcState(val mode: AcMode, val temperature: Int? = null, val fan: AcFan? = null) {
    companion object {
        const val MIN_TEMP = 16
        const val MAX_TEMP = 32

        private val SPLIT = Regex("[^a-z0-9]+")

        /**
         * A best guess from a button's name, for buttons that weren't given a state: the names AC files use, such
         * as "Off", "Cool_hi", "Heat_lo", "Dh" or "Cool 22". Null when the name doesn't say.
         */
        fun guess(label: String): AcState? {
            val words = SPLIT.split(label.lowercase(Locale.ROOT)).filter { it.isNotEmpty() }
            if (words.isEmpty()) return null
            val mode = when {
                words.any { it == "off" || it == "poweroff" } -> AcMode.OFF
                words.any { it == "cool" || it == "cooling" || it == "cold" } -> AcMode.COOL
                words.any { it == "heat" || it == "heating" || it == "warm" } -> AcMode.HEAT
                words.any { it == "dry" || it == "dh" || it == "dehumidify" } -> AcMode.DRY
                words.any { it == "fan" && words.size == 1 } || words.any { it == "fanonly" || it == "ventilate" } -> AcMode.FAN
                words.any { it == "auto" } && words.none { it == "fan" } -> AcMode.AUTO
                else -> return null
            }
            if (mode == AcMode.OFF) return AcState(AcMode.OFF)
            val temp = words.mapNotNull { it.removeSuffix("c").toIntOrNull() }.firstOrNull { it in MIN_TEMP..MAX_TEMP }
            val fan = when {
                words.any { it == "hi" || it == "high" || it == "max" } -> AcFan.HIGH
                words.any { it == "mid" || it == "med" || it == "medium" } -> AcFan.MEDIUM
                words.any { it == "lo" || it == "low" || it == "min" } -> AcFan.LOW
                else -> null
            }
            return AcState(mode, temp, fan)
        }
    }
}

/** The state [button] sets: the one given in the editor, or else a guess from its name. */
fun acStateOf(button: RemoteButton): AcState? = button.ac ?: AcState.guess(button.label)

/**
 * Finding the button that goes from the remembered state to the one asked for. Every button with a state (given
 * or guessed) is a candidate; nothing is sent that isn't one of the remote's own codes.
 */
object AcControl {
    fun states(remote: Remote): List<Pair<RemoteButton, AcState>> =
        remote.buttons.mapNotNull { b -> acStateOf(b)?.let { b to it } }

    /** One degree up ([delta] > 0) or down in the same mode (and fan, when known): the nearest temperature that way. */
    fun step(remote: Remote, current: AcState?, delta: Int): RemoteButton? {
        val cur = current ?: return null
        val t = cur.temperature ?: return null
        val same = states(remote).filter { (_, s) -> s.mode == cur.mode && s.temperature != null }
        val pool = same.filter { (_, s) -> cur.fan == null || s.fan == cur.fan }.ifEmpty { same }
        return if (delta > 0) pool.filter { it.second.temperature!! > t }.minByOrNull { it.second.temperature!! }?.first
        else pool.filter { it.second.temperature!! < t }.maxByOrNull { it.second.temperature!! }?.first
    }

    /** A button for [mode], keeping the temperature and fan when the remote has such a button. */
    fun forMode(remote: Remote, current: AcState?, mode: AcMode): RemoteButton? {
        val pool = states(remote).filter { it.second.mode == mode }
        if (pool.isEmpty()) return null
        return pool.maxByOrNull { (_, s) ->
            (if (current != null && s.temperature != null && s.temperature == current.temperature) 2 else 0) +
                (if (current != null && s.fan != null && s.fan == current.fan) 1 else 0)
        }?.first
    }

    /** A button for [fan] in the current mode, keeping the temperature when the remote has such a button. */
    fun forFan(remote: Remote, current: AcState?, fan: AcFan): RemoteButton? {
        val cur = current ?: return null
        val pool = states(remote).filter { it.second.mode == cur.mode && it.second.fan == fan }
        return (pool.firstOrNull { it.second.temperature == cur.temperature } ?: pool.firstOrNull())?.first
    }

    fun modes(remote: Remote): List<AcMode> = states(remote).map { it.second.mode }.distinct().sortedBy { it.ordinal }

    fun fans(remote: Remote, mode: AcMode): List<AcFan> =
        states(remote).filter { it.second.mode == mode }.mapNotNull { it.second.fan }.distinct().sortedBy { it.ordinal }
}
