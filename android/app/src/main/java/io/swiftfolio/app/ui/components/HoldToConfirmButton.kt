package io.swiftfolio.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private const val HOLD_MILLIS = 600
private const val RELEASE_MILLIS = 150
private const val LABEL_FLIP_FRACTION = 0.5f

/**
 * Destructive button that fires only after a [HOLD_MILLIS] press that fills it left to
 * right. A short tap shows [holdHint] instead. Accessibility services get a direct click
 * action, so TalkBack users are never forced to hold.
 */
@Composable
fun HoldToConfirmButton(
    text: String,
    holdHint: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ConsoleTheme.colors
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val confirm by rememberUpdatedState(onConfirm)
    var showHint by remember { mutableStateOf(false) }
    val labelColor = if (progress.value > LABEL_FLIP_FRACTION) colors.onDestructive else colors.error
    Box(
        modifier = modifier
            .heightIn(min = Spacing.touchTarget)
            .clip(CircleShape)
            .background(colors.glass4)
            .drawBehind { drawRect(colors.destructiveContainer, size = Size(size.width * progress.value, size.height)) }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    val fill = scope.launch {
                        progress.animateTo(1f, tween(HOLD_MILLIS, easing = LinearEasing))
                        confirm()
                    }
                    tryAwaitRelease()
                    if (progress.value < 1f) {
                        fill.cancel()
                        showHint = true
                        scope.launch { progress.animateTo(0f, tween(RELEASE_MILLIS)) }
                    }
                })
            }
            .semantics {
                role = Role.Button
                onClick(label = text) {
                    if (enabled) confirm()
                    enabled
                }
            }
            .padding(horizontal = Spacing.space5),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (showHint && progress.value == 0f) holdHint else text, color = labelColor, style = MaterialTheme.typography.labelLarge)
    }
}
