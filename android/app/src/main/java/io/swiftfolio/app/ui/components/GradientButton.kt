package io.swiftfolio.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing

private val SpinnerSize = 18.dp
private val SpinnerStroke = 2.dp
private val ButtonIconSize = 18.dp

/**
 * Primary action: a pill filled with the brand gradient. Disabled state drops the gradient
 * for a flat glass3 fill. While [loading], the label crossfades into a spinner plus
 * [loadingLabel].
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String? = null,
    trailingIcon: ImageVector? = null,
    minHeight: Dp = Spacing.primaryButtonHeight,
) {
    val colors = ConsoleTheme.colors
    val enterMillis = ConsoleTheme.motion.quickMillis
    val exitMillis = ConsoleTheme.motion.exitMillis
    val active = enabled && !loading
    val fill = if (enabled || loading) Modifier.background(colors.accentBrush, CircleShape) else Modifier.background(colors.glass3, CircleShape)
    Button(
        onClick = onClick,
        enabled = active,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = colors.onAccent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = if (loading) colors.onAccent else colors.textDisabled,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.space6),
        modifier = modifier.heightIn(min = minHeight).then(fill),
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(enterMillis)) togetherWith fadeOut(tween(exitMillis)) },
            label = "gradientButtonContent",
        ) { isLoading ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(SpinnerSize), color = colors.onAccent, strokeWidth = SpinnerStroke)
                    loadingLabel?.let { Text(it, style = ConsoleTheme.mono.path) }
                } else {
                    Text(text)
                    trailingIcon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(ButtonIconSize)) }
                }
            }
        }
    }
}
