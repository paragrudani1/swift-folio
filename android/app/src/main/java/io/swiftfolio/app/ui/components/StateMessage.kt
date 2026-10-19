package io.swiftfolio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing

private val BadgeSize = 72.dp
private val BadgeIconSize = 32.dp
private val BadgeShape = RoundedCornerShape(20.dp)

/**
 * Centered empty / error / no-results message: a 72dp glass badge with an icon, a title,
 * an optional mono detail line and an optional row of actions.
 */
@Composable
fun StateMessage(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    iconTint: Color = ConsoleTheme.colors.textTertiary,
    detail: String? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = ConsoleTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.space6, vertical = Spacing.space8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.space3),
    ) {
        Box(
            Modifier.size(BadgeSize).background(colors.glass2, BadgeShape).border(Spacing.hairline, colors.rimBrush, BadgeShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(BadgeIconSize))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
        detail?.let {
            Text(it, style = ConsoleTheme.mono.meta, color = colors.textTertiary, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) { actions() }
    }
}
