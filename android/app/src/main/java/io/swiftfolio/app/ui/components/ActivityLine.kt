package io.swiftfolio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.ui.theme.ConsoleTheme

private val LineHeight = 2.dp
private const val SWEEP_MILLIS = 1100
private const val SEGMENT_FRACTION = 0.35f
private const val STATIC_FRACTION = 0.5f

/**
 * The "console is busy" signal: a 2dp indeterminate gradient segment sweeping across.
 * With reduced motion it becomes a static half-width bar.
 */
@Composable
fun ActivityLine(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val colors = ConsoleTheme.colors
        val reduced = ConsoleTheme.motion.reduced
        val transition = rememberInfiniteTransition(label = "activity")
        val phase by transition.animateFloat(
            initialValue = -SEGMENT_FRACTION,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(SWEEP_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "activityPhase",
        )
        val brush = Brush.horizontalGradient(listOf(colors.accentGlowStart, colors.accentGlowEnd))
        Box(
            Modifier.fillMaxWidth().height(LineHeight).drawBehind {
                val start = if (reduced) 0f else phase * size.width
                val width = size.width * (if (reduced) STATIC_FRACTION else SEGMENT_FRACTION)
                drawRect(brush = brush, topLeft = Offset(start, 0f), size = Size(width, size.height))
            },
        )
    }
}
