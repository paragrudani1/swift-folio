package io.swiftfolio.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import io.swiftfolio.app.ui.theme.ConsoleTheme

private const val HIGHLIGHT_ALPHA = 0.22f

/** Single-line text with [ranges] highlighted (search matches). Ranges come from core `Listing.matchRanges`. */
@Composable
fun HighlightedText(
    text: String,
    ranges: List<IntRange>,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = ConsoleTheme.colors
    val highlight = SpanStyle(background = colors.accentStart.copy(alpha = HIGHLIGHT_ALPHA), color = colors.textPrimary)
    val annotated = remember(text, ranges, highlight) {
        buildAnnotatedString {
            append(text)
            ranges.forEach { range -> addStyle(highlight, range.first, range.last + 1) }
        }
    }
    Text(annotated, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}
