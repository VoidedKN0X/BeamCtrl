package com.vknxworks.beamctrl.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vknxworks.beamctrl.R

/**
 * Ctrl / Signal, the look shared by every Ctrl app: near-black, high contrast, one accent.
 * Dark only; there is no light theme and no wallpaper colours.
 */
object CtrlColors {
    /** The one accent colour. Change it here and the whole app follows. */
    val Accent = Color(0xFFB48CFF)

    val Background = Color(0xFF08080A)
    val Surface = Color(0xFF141417)
    val Raised = Color(0xFF1C1C21)
    /** Card borders. */
    val Outline = Color(0xFF232328)
    /** Borders on controls: avatar, back button, text fields, the bottom bar. */
    val ControlOutline = Color(0xFF2A2A2F)
    /** Lines between list rows. */
    val Divider = Color(0xFF1E1E23)
    val Text = Color(0xFFF4F4F2)
    val Muted = Color(0xFFA3A3AB)
    /** Disabled or stopped. */
    val Inactive = Color(0xFF8C8C94)
    val SwitchTrackOff = Color(0xFF2A2A30)
    val ProgressTrack = Color(0xFF26262C)
    /** Empty PIN cells. */
    val EmptyCell = Color(0xFF3A3A41)
    val Error = Color(0xFFFF6B6B)
    /** Text and icons on the accent. */
    val OnAccent = Background
}

private val Scheme = darkColorScheme(
    primary = CtrlColors.Accent,
    onPrimary = CtrlColors.OnAccent,
    primaryContainer = CtrlColors.Accent,
    onPrimaryContainer = CtrlColors.OnAccent,
    inversePrimary = CtrlColors.Accent,
    secondary = CtrlColors.Text,
    onSecondary = CtrlColors.Background,
    secondaryContainer = CtrlColors.Raised,
    onSecondaryContainer = CtrlColors.Text,
    tertiary = CtrlColors.Accent,
    onTertiary = CtrlColors.OnAccent,
    tertiaryContainer = CtrlColors.Raised,
    onTertiaryContainer = CtrlColors.Text,
    background = CtrlColors.Background,
    onBackground = CtrlColors.Text,
    surface = CtrlColors.Background,
    onSurface = CtrlColors.Text,
    surfaceVariant = CtrlColors.Raised,
    onSurfaceVariant = CtrlColors.Muted,
    surfaceTint = Color.Transparent,
    surfaceBright = CtrlColors.Raised,
    surfaceDim = CtrlColors.Background,
    surfaceContainerLowest = CtrlColors.Background,
    surfaceContainerLow = CtrlColors.Surface,
    surfaceContainer = CtrlColors.Surface,
    surfaceContainerHigh = CtrlColors.Raised,
    surfaceContainerHighest = CtrlColors.Raised,
    inverseSurface = CtrlColors.Text,
    inverseOnSurface = CtrlColors.Background,
    outline = CtrlColors.ControlOutline,
    outlineVariant = CtrlColors.Outline,
    error = CtrlColors.Error,
    onError = CtrlColors.OnAccent,
    errorContainer = CtrlColors.Error,
    onErrorContainer = CtrlColors.OnAccent,
    scrim = Color.Black,
)

// Space Grotesk (variable, weights 300 to 700) for all UI text, JetBrains Mono for numbers, data and labels.
// Both are bundled (SIL Open Font License 1.1); nothing is downloaded.
private fun grotesk(weight: Int) = Font(
    R.font.space_grotesk,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val SpaceGrotesk = FontFamily(grotesk(400), grotesk(500), grotesk(600), grotesk(700))

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

private fun style(size: Int, line: Int, weight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = SpaceGrotesk,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.em,
)

private fun mono(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = JetBrainsMono,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
)

/** The mono styles that Material's type scale has no slot for. */
object CtrlType {
    /** Section labels: always UPPERCASE, muted. */
    val Label = mono(11, 14, FontWeight.Normal, 0.16)
    /** Labels on the accent (hero card). */
    val LabelStrong = mono(11, 14, FontWeight.SemiBold, 0.16)
    /** "BEAM/CTRL" in the top bar. */
    val Wordmark = mono(13, 16, FontWeight.SemiBold, 0.14)
    /** Small data readouts: counts, codes, protocols, versions. */
    val Data = mono(12, 16, FontWeight.Normal)
    /** Trailing status in list rows. */
    val Status = mono(11, 14, FontWeight.Medium, 0.1)
    /** Keypad digits. */
    val Key = mono(26, 30, FontWeight.Medium)
    /** Big readouts in JetBrains Mono. */
    val Readout = mono(40, 44, FontWeight.SemiBold, -0.04)
    /** The giant number on a hero card. */
    val Hero = style(64, 60, 700, -0.05)
}

private val AppTypography = Typography(
    displayLarge = style(64, 60, 700, -0.05),
    displayMedium = style(56, 56, 700, -0.04),
    displaySmall = style(40, 42, 700, -0.035),
    headlineLarge = style(40, 42, 700, -0.035),
    headlineMedium = style(30, 34, 700, -0.03),
    headlineSmall = style(24, 30, 700, -0.02),
    titleLarge = style(18, 24, 600),
    titleMedium = style(16, 22, 600),
    titleSmall = style(14, 20, 600),
    bodyLarge = style(16, 24, 400),
    bodyMedium = style(14, 20, 400),
    bodySmall = style(12, 17, 400),
    labelLarge = style(14, 20, 600),
    labelMedium = style(12, 16, 500),
    labelSmall = mono(11, 14, FontWeight.Medium, 0.1),
)

/** Shapes: cards 22dp, hero cards and sheets 26dp, controls 14 to 16dp, chips 10dp. */
object CtrlShapes {
    val Card = RoundedCornerShape(22.dp)
    val Hero = RoundedCornerShape(26.dp)
    val Sheet = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    val Dialog = RoundedCornerShape(26.dp)
    val Button = RoundedCornerShape(18.dp)
    val Control = RoundedCornerShape(14.dp)
    val Field = RoundedCornerShape(16.dp)
    val Chip = RoundedCornerShape(10.dp)
    val Key = RoundedCornerShape(22.dp)
    val Dot = RoundedCornerShape(2.dp)
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/** Horizontal page margin used everywhere. */
val Gutter = 20.dp

@Composable
fun BeamCtrlTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
