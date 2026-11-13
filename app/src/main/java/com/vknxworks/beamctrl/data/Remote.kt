package com.vknxworks.beamctrl.data

import androidx.compose.material.icons.Icons
import com.vknxworks.beamctrl.R
import androidx.annotation.StringRes
import android.content.Context
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Dvr
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.vknxworks.beamctrl.ir.IrCode
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class DeviceType(@StringRes val label: Int) {
    TV(R.string.type_tv),
    BOX(R.string.type_box),
    SOUNDBAR(R.string.type_soundbar),
    PROJECTOR(R.string.type_projector),
    AC(R.string.type_ac),
    FAN(R.string.type_fan),
    LIGHT(R.string.type_light),
    OTHER(R.string.type_other);

    val icon: ImageVector
        get() = when (this) {
            TV -> Icons.Outlined.Tv
            BOX -> Icons.Outlined.Dvr
            SOUNDBAR -> Icons.Outlined.Speaker
            PROJECTOR -> Icons.Outlined.Videocam
            AC -> Icons.Outlined.AcUnit
            FAN -> Icons.Outlined.Air
            LIGHT -> Icons.Outlined.Lightbulb
            OTHER -> Icons.Outlined.SettingsRemote
        }
}

/**
 * Where a button sits on the remote layout. Buttons without a role (or with one the layout
 * doesn't have room for twice) go in the "More buttons" grid.
 */
@Serializable
enum class KeyRole(@StringRes val labelRes: Int, val number: Int? = null) {
    POWER(R.string.key_power),
    INPUT(R.string.key_input),
    MUTE(R.string.key_mute),
    VOL_UP(R.string.key_vol_up),
    VOL_DOWN(R.string.key_vol_down),
    CH_UP(R.string.key_ch_up),
    CH_DOWN(R.string.key_ch_down),
    UP(R.string.key_up),
    DOWN(R.string.key_down),
    LEFT(R.string.key_left),
    RIGHT(R.string.key_right),
    OK(R.string.key_ok),
    BACK(R.string.key_back),
    HOME(R.string.key_home),
    MENU(R.string.key_menu),
    EXIT(R.string.key_exit),
    INFO(R.string.key_info),
    GUIDE(R.string.key_guide),
    PLAY(R.string.key_play),
    PAUSE(R.string.key_pause),
    PLAY_PAUSE(R.string.key_play_pause),
    STOP(R.string.key_stop),
    REWIND(R.string.key_rewind),
    FAST_FORWARD(R.string.key_fast_forward),
    DIGIT_1(0, 1), DIGIT_2(0, 2), DIGIT_3(0, 3),
    DIGIT_4(0, 4), DIGIT_5(0, 5), DIGIT_6(0, 6),
    DIGIT_7(0, 7), DIGIT_8(0, 8), DIGIT_9(0, 9),
    DIGIT_0(0, 0);

    /** The key's name in the phone's language ("Volume up"), or its number. */
    fun label(context: Context): String = number?.toString() ?: context.getString(labelRes)

    companion object {
        fun digit(n: Int): KeyRole = when (n) {
            0 -> DIGIT_0
            else -> entries[DIGIT_1.ordinal + n - 1]
        }
    }
}

@Serializable
data class RemoteButton(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val role: KeyRole? = null,
    val code: IrCode,
)

@Serializable
data class Remote(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: DeviceType = DeviceType.TV,
    val brand: String? = null,
    val buttons: List<RemoteButton> = emptyList(),
    val created: Long = System.currentTimeMillis(),
    val lastUsed: Long = 0,
) {
    fun button(role: KeyRole): RemoteButton? = buttons.firstOrNull { it.role == role }
}
