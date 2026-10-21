package com.vknxworks.beamctrl.ui.remote

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vknxworks.beamctrl.container
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.RemoteButton

/** How keys on the current screen behave: sending codes, or (in edit mode) opening the editor. */
class KeyHost(
    val editing: Boolean = false,
    val holdToRepeat: Boolean = true,
    val haptics: Boolean = true,
    val onEdit: (button: RemoteButton?, role: KeyRole?) -> Unit = { _, _ -> },
    val onSent: () -> Unit = {},
)

val LocalKeyHost = compositionLocalOf { KeyHost() }

/**
 * One key on a remote. Pressing it sends the button's code straight away; with [repeat],
 * holding it keeps sending (volume, channels, arrows). Missing buttons show dimmed, and in edit
 * mode tapping any key, present or missing, opens the editor for that spot.
 */
@Composable
fun BeamKey(
    button: RemoteButton?,
    role: KeyRole?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    repeat: Boolean = false,
    label: String? = button?.label ?: role?.label(LocalContext.current),
    content: @Composable BoxScope.() -> Unit,
) {
    val host = LocalKeyHost.current
    val blaster = LocalContext.current.container.blaster
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val current by rememberUpdatedState(button)

    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.55f, stiffness = 900f), label = "key-scale")
    val bg by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f) else container,
        label = "key-bg",
    )
    val enabled = button != null || host.editing

    val input = when {
        host.editing -> Modifier.clickable { host.onEdit(button, role) }
        button != null -> Modifier.pointerInput(button.id, host.holdToRepeat, repeat) {
            detectTapGestures(
                onPress = {
                    val b = current ?: return@detectTapGestures
                    pressed = true
                    if (host.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val press = blaster.press(b.code, repeat && host.holdToRepeat)
                    host.onSent()
                    try {
                        tryAwaitRelease()
                    } finally {
                        press.release()
                        pressed = false
                    }
                },
            )
        }
        else -> Modifier
    }

    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.32f
            }
            .clip(shape)
            .background(bg)
            .then(if (host.editing) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (button != null) 0.7f else 0.3f), shape) else Modifier)
            .then(input)
            .semantics {
                this.role = Role.Button
                if (label != null) this.contentDescription = label
                if (button != null && !host.editing) onClick(label) { blaster.send(button.code); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}
