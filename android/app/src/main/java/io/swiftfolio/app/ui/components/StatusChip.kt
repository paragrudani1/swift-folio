package io.swiftfolio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing

private val ChipHeight = 24.dp
private val DotSize = 6.dp

/** Compact 24dp mono-caps chip, optionally led by a status dot (CONNECTED, region, totals). */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    textColor: Color = ConsoleTheme.colors.textSecondary,
) {
    val colors = ConsoleTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = ChipHeight)
            .background(colors.glass2, CircleShape)
            .border(Spacing.hairline, colors.border, CircleShape)
            .padding(horizontal = Spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.space1 + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dotColor?.let { Box(Modifier.size(DotSize).background(it, CircleShape)) }
        Text(text = text.uppercase(), style = ConsoleTheme.mono.caps, color = textColor)
    }
}
