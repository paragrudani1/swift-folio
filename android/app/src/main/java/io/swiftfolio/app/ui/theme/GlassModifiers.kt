package io.swiftfolio.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private const val FLOATING_GLASS_ALPHA = 0.92f
private const val AURORA_RADIUS_DP = 420f
private val FloatingShadowElevation = 12.dp

/**
 * Fixed background "aurora": two faint radial glows (top-left blue, bottom-right violet)
 * drawn once behind the content. Not animated.
 */
@Composable
fun Modifier.aurora(): Modifier {
    val colors = ConsoleTheme.colors
    return drawBehind {
        drawRect(colors.canvas)
        val radius = AURORA_RADIUS_DP * density
        drawRect(Brush.radialGradient(listOf(colors.auroraStart, colors.auroraStart.copy(alpha = 0f)), Offset.Zero, radius))
        drawRect(
            Brush.radialGradient(
                listOf(colors.auroraEnd, colors.auroraEnd.copy(alpha = 0f)),
                Offset(size.width, size.height),
                radius,
            ),
        )
    }
}

/** Opaque glass card: tier colour plus 1dp hairline border. */
@Composable
fun Modifier.glass(shape: Shape, tier: GlassTier = GlassTier.Two): Modifier {
    val colors = ConsoleTheme.colors
    val fill = when (tier) {
        GlassTier.One -> colors.glass1
        GlassTier.Two -> colors.glass2
        GlassTier.Three -> colors.glass3
        GlassTier.Four -> colors.glass4
    }
    return background(fill, shape).border(Spacing.hairline, colors.border, shape)
}

/**
 * Floating glass for chrome over scrolling content (command bar, path pill): glass3 at 92%
 * with the luminous top rim, plus a soft blue-tinted shadow in light theme. This is the
 * documented "tier 2" fallback used on every API level (no live backdrop blur).
 */
@Composable
fun Modifier.floatingGlass(shape: Shape): Modifier {
    val colors = ConsoleTheme.colors
    val shadowed = if (colors.isDark) {
        this
    } else {
        shadow(FloatingShadowElevation, shape, clip = false, ambientColor = colors.shadowAmbient, spotColor = colors.shadowSpot)
    }
    return shadowed
        .background(colors.glass3.copy(alpha = FLOATING_GLASS_ALPHA), shape)
        .border(Spacing.hairline, colors.rimBrush, shape)
}

/** Glass tiers from DIRECTION-C.md 2.1. */
enum class GlassTier { One, Two, Three, Four }
