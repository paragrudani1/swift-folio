package io.swiftfolio.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalConsoleColors = staticCompositionLocalOf { DarkConsoleColors }
private val LocalConsoleMono = staticCompositionLocalOf { ConsoleMonoType() }
private val LocalConsoleMotion = staticCompositionLocalOf { ConsoleMotion() }

/** Accessors for the Console tokens that Material 3 has no slot for. */
object ConsoleTheme {
    /** Colour tokens of the current theme. */
    val colors: ConsoleColors
        @Composable @ReadOnlyComposable get() = LocalConsoleColors.current

    /** Mono text styles. */
    val mono: ConsoleMonoType
        @Composable @ReadOnlyComposable get() = LocalConsoleMono.current

    /** Motion tokens (already adjusted for reduced motion). */
    val motion: ConsoleMotion
        @Composable @ReadOnlyComposable get() = LocalConsoleMotion.current
}

/**
 * Root theme. Dynamic colour is intentionally off: the brand gradient is the identity.
 *
 * @param darkTheme resolved theme (System/Light/Dark already applied by the caller).
 * @param reduceMotion true when system animations are disabled.
 */
@Composable
fun SwiftFolioTheme(
    darkTheme: Boolean,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkConsoleColors else LightConsoleColors
    CompositionLocalProvider(
        LocalConsoleColors provides colors,
        LocalConsoleMono provides ConsoleMonoType(),
        LocalConsoleMotion provides ConsoleMotion(reduced = reduceMotion),
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = ConsoleTypography,
            shapes = ConsoleShapes,
            content = content,
        )
    }
}

/** Material mapping from DIRECTION-C.md 2.3 so stock components inherit the theme. */
private fun ConsoleColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accentStart,
        onPrimary = onAccent,
        primaryContainer = glass4,
        onPrimaryContainer = textPrimary,
        secondary = accentEnd,
        onSecondary = onAccent,
        secondaryContainer = glass4,
        onSecondaryContainer = textPrimary,
        background = canvas,
        onBackground = textPrimary,
        surface = canvas,
        onSurface = textPrimary,
        surfaceVariant = glass2,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = canvas,
        surfaceContainerLow = glass1,
        surfaceContainer = glass2,
        surfaceContainerHigh = glass3,
        surfaceContainerHighest = glass4,
        inverseSurface = textPrimary,
        inverseOnSurface = canvas,
        outline = borderStrong,
        outlineVariant = border,
        error = error,
        onError = onDestructive,
        scrim = scrim,
    )
}
