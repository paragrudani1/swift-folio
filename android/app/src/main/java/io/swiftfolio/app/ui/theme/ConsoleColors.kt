package io.swiftfolio.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Console colour tokens (DIRECTION-C.md section 2). Glass tiers are the pre-blended
 * "effective" opaque values; alpha is only used where content really shows through.
 */
@Immutable
data class ConsoleColors(
    val isDark: Boolean,
    val canvas: Color,
    val auroraStart: Color,
    val auroraEnd: Color,
    val glass1: Color,
    val glass2: Color,
    val glass3: Color,
    val glass4: Color,
    val scrim: Color,
    val border: Color,
    val borderStrong: Color,
    val rimTop: Color,
    val rimBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val accentStart: Color,
    val accentEnd: Color,
    val accentGlowStart: Color,
    val accentGlowEnd: Color,
    val accentText: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val destructiveContainer: Color,
    val onDestructive: Color,
    val shadowAmbient: Color,
    val shadowSpot: Color,
    val shimmerHighlight: Color,
) {
    /** The brand gradient used for primary actions, progress and the active selection. */
    val accentBrush: Brush get() = Brush.horizontalGradient(listOf(accentStart, accentEnd))

    /** Decorative glow gradient (focus rings, progress shimmer). */
    val glowBrush: Brush get() = Brush.horizontalGradient(listOf(accentGlowStart, accentGlowEnd))

    /** Top-lit 1dp rim for glass3 surfaces. */
    val rimBrush: Brush get() = Brush.verticalGradient(listOf(rimTop, rimBottom))
}

/** Raw palette values; referenced only by the two token sets below. */
private object Palette {
    val accentStart = Color(0xFF2E5BFF)
    val accentEnd = Color(0xFF6E44FF)
    val glowStart = Color(0xFF4F8BFF)
    val glowEnd = Color(0xFF9B6BFF)
    val ink = Color(0xFF0B1220)
    const val AURORA_DARK_START = 0.07f
    const val AURORA_DARK_END = 0.05f
    const val AURORA_LIGHT_START = 0.05f
    const val AURORA_LIGHT_END = 0.04f
}

/** Dark (default) tokens. */
val DarkConsoleColors = ConsoleColors(
    isDark = true,
    canvas = Color(0xFF07090E),
    auroraStart = Palette.accentStart.copy(alpha = Palette.AURORA_DARK_START),
    auroraEnd = Palette.accentEnd.copy(alpha = Palette.AURORA_DARK_END),
    glass1 = Color(0xFF111318),
    glass2 = Color(0xFF181A1F),
    glass3 = Color(0xFF222429),
    glass4 = Color(0xFF2F3035),
    scrim = Color.Black.copy(alpha = 0.60f),
    border = Color.White.copy(alpha = 0.08f),
    borderStrong = Color.White.copy(alpha = 0.14f),
    rimTop = Color.White.copy(alpha = 0.18f),
    rimBottom = Color.White.copy(alpha = 0.04f),
    textPrimary = Color(0xFFE8ECF4),
    textSecondary = Color(0xFFA4AEC2),
    textTertiary = Color(0xFF8590A6),
    textDisabled = Color.White.copy(alpha = 0.38f),
    accentStart = Palette.accentStart,
    accentEnd = Palette.accentEnd,
    accentGlowStart = Palette.glowStart,
    accentGlowEnd = Palette.glowEnd,
    accentText = Color(0xFF7FA8FF),
    onAccent = Color.White,
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    error = Color(0xFFFB7185),
    destructiveContainer = Color(0xFFFB7185),
    onDestructive = Color(0xFF1A0006),
    shadowAmbient = Color.Transparent,
    shadowSpot = Color.Transparent,
    shimmerHighlight = Color.White.copy(alpha = 0.06f),
)

/** Light ("frosted") tokens. */
val LightConsoleColors = ConsoleColors(
    isDark = false,
    canvas = Color(0xFFF3F5F9),
    auroraStart = Palette.accentStart.copy(alpha = Palette.AURORA_LIGHT_START),
    auroraEnd = Palette.accentEnd.copy(alpha = Palette.AURORA_LIGHT_END),
    glass1 = Color(0xFFFAFBFC),
    glass2 = Color(0xFFFCFCFE),
    glass3 = Color(0xFFFEFEFE),
    glass4 = Color(0xFFEBEFFE),
    scrim = Palette.ink.copy(alpha = 0.32f),
    border = Palette.ink.copy(alpha = 0.08f),
    borderStrong = Palette.ink.copy(alpha = 0.14f),
    rimTop = Color.White,
    rimBottom = Palette.ink.copy(alpha = 0.06f),
    textPrimary = Palette.ink,
    textSecondary = Color(0xFF465169),
    textTertiary = Color(0xFF5A647A),
    textDisabled = Palette.ink.copy(alpha = 0.38f),
    accentStart = Palette.accentStart,
    accentEnd = Palette.accentEnd,
    accentGlowStart = Palette.glowStart,
    accentGlowEnd = Palette.glowEnd,
    accentText = Color(0xFF2F5BEA),
    onAccent = Color.White,
    success = Color(0xFF15803D),
    warning = Color(0xFFB45309),
    error = Color(0xFFBE123C),
    destructiveContainer = Color(0xFFBE123C),
    onDestructive = Color.White,
    shadowAmbient = Palette.ink.copy(alpha = 0.06f),
    shadowSpot = Palette.accentStart.copy(alpha = 0.10f),
    shimmerHighlight = Color.White.copy(alpha = 0.70f),
)
