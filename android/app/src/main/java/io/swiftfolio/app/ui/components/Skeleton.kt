package io.swiftfolio.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.app.ui.theme.TileShape

private val BarShape = RoundedCornerShape(6.dp)
private const val NAME_WIDTH_FRACTION = 0.6f
private const val META_WIDTH_FRACTION = 0.35f
private const val SHIMMER_SPAN = 600f

/** Diagonal shimmer brush shared by skeleton shapes; a static fill when motion is reduced. */
@Composable
fun rememberShimmerBrush(): Brush {
    val colors = ConsoleTheme.colors
    val duration = ConsoleTheme.motion.shimmerMillis ?: return SolidColor(colors.glass2)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -SHIMMER_SPAN,
        targetValue = SHIMMER_SPAN * 2,
        animationSpec = infiniteRepeatable(tween(duration, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerOffset",
    )
    return Brush.linearGradient(
        colors = listOf(colors.glass2, colors.shimmerHighlight, colors.glass2),
        start = Offset(offset, offset),
        end = Offset(offset + SHIMMER_SPAN, offset + SHIMMER_SPAN),
    )
}

/** Placeholder row shown while a folder loads: tile plus two bars. */
@Composable
fun SkeletonRow(brush: Brush, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.fileRowMinHeight)
            .padding(horizontal = Spacing.space5, vertical = Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space3),
    ) {
        Box(Modifier.size(Spacing.tileSize).background(brush, TileShape))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Box(Modifier.fillMaxWidth(NAME_WIDTH_FRACTION).height(12.dp).background(brush, BarShape))
            Box(Modifier.fillMaxWidth(META_WIDTH_FRACTION).height(10.dp).background(brush, BarShape))
        }
    }
}
