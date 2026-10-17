package com.vknxworks.beamctrl.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Dvr
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.vknxworks.beamctrl.ir.IrCode
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class DeviceType(val label: String) {
    TV("TV"),
    BOX("Media box"),
    SOUNDBAR("Soundbar / speaker"),
    PROJECTOR("Projector"),
    AC("Air conditioner"),
    FAN("Fan"),
    LIGHT("Light"),
    OTHER("Other");

    val icon: ImageVector
        get() = when (this) {
            TV -> Icons.Filled.Tv
            BOX -> Icons.Filled.Dvr
            SOUNDBAR -> Icons.Filled.Speaker
            PROJECTOR -> Icons.Filled.Videocam
            AC -> Icons.Filled.AcUnit
            FAN -> Icons.Filled.Air
            LIGHT -> Icons.Filled.Lightbulb
            OTHER -> Icons.Filled.SettingsRemote
        }
}

/**
 * Where a button sits on the remote layout. Buttons without a role (or with one the layout
 * doesn't have room for twice) go in the "More buttons" grid.
 */
@Serializable
enum class KeyRole(val label: String) {
    POWER("Power"),
    INPUT("Input"),
    MUTE("Mute"),
    VOL_UP("Volume up"),
    VOL_DOWN("Volume down"),
    CH_UP("Channel up"),
    CH_DOWN("Channel down"),
    UP("Up"),
    DOWN("Down"),
    LEFT("Left"),
    RIGHT("Right"),
    OK("OK"),
    BACK("Back"),
    HOME("Home"),
    MENU("Menu"),
    EXIT("Exit"),
    INFO("Info"),
    GUIDE("Guide"),
    PLAY("Play"),
    PAUSE("Pause"),
    PLAY_PAUSE("Play / pause"),
    STOP("Stop"),
    REWIND("Rewind"),
    FAST_FORWARD("Fast forward"),
    DIGIT_1("1"), DIGIT_2("2"), DIGIT_3("3"),
    DIGIT_4("4"), DIGIT_5("5"), DIGIT_6("6"),
    DIGIT_7("7"), DIGIT_8("8"), DIGIT_9("9"),
    DIGIT_0("0");

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
