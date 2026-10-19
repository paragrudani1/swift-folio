package io.swiftfolio.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.app.ui.theme.TileShape

private val GlyphSize = 20.dp
private val CheckSize = 24.dp
private val CheckGlyphSize = 16.dp
private val UncheckedRingWidth = 2.dp
private const val SELECTION_DAMPING = 0.55f
private const val SELECTION_STIFFNESS = 600f
private const val CHECK_START_SCALE = 0.6f

/** What the leading tile of a row shows. */
enum class TileMode { TYPE, UNCHECKED, CHECKED }

/**
 * 36dp file-type tile. In selection mode it crossfades into a check circle; the check pops
 * with the app's single springy animation.
 */
@Composable
fun FileTypeTile(style: TileStyle, mode: TileMode, modifier: Modifier = Modifier) {
    val colors = ConsoleTheme.colors
    val motion = ConsoleTheme.motion
    AnimatedContent(
        targetState = mode,
        transitionSpec = { fadeIn(tween(motion.quickMillis)) togetherWith fadeOut(tween(motion.exitMillis)) },
        label = "tileMode",
        modifier = modifier.size(Spacing.tileSize),
    ) { target ->
        Box(Modifier.size(Spacing.tileSize), contentAlignment = Alignment.Center) {
            when (target) {
                TileMode.TYPE -> Box(
                    Modifier.size(Spacing.tileSize).background(colors.glass2, TileShape).border(Spacing.hairline, colors.border, TileShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(style.icon, contentDescription = stringResource(style.labelRes), tint = style.tint(colors), modifier = Modifier.size(GlyphSize))
                }
                TileMode.UNCHECKED -> Box(Modifier.size(CheckSize).border(UncheckedRingWidth, colors.borderStrong, CircleShape))
                TileMode.CHECKED -> SelectedCheck()
            }
        }
    }
}

@Composable
private fun SelectedCheck() {
    val colors = ConsoleTheme.colors
    val reduced = ConsoleTheme.motion.reduced
    val scale = remember { Animatable(if (reduced) 1f else CHECK_START_SCALE) }
    LaunchedEffect(reduced) {
        if (!reduced) scale.animateTo(1f, spring(dampingRatio = SELECTION_DAMPING, stiffness = SELECTION_STIFFNESS))
    }
    Box(
        Modifier.size(CheckSize).scale(scale.value).background(colors.accentBrush, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(CheckGlyphSize))
    }
}
