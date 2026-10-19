package io.swiftfolio.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Immutable

/**
 * Motion tokens (DIRECTION-C.md 4.4): short and decelerating, no bounce except the
 * selection tick. When [reduced] is true every duration collapses to [instantMillis] or 0.
 */
@Immutable
data class ConsoleMotion(val reduced: Boolean = false) {
    /** Press states, chip toggles. */
    val instantMillis: Int get() = if (reduced) 0 else INSTANT

    /** Row enter, icon morphs, command bar mode swap. */
    val quickMillis: Int get() = if (reduced) INSTANT else QUICK

    /** Folder navigation, sheet open. */
    val standardMillis: Int get() = if (reduced) INSTANT else STANDARD

    /** Anything leaving the screen. */
    val exitMillis: Int get() = if (reduced) 0 else EXIT

    /** Skeleton shimmer loop; null disables the shimmer. */
    val shimmerMillis: Int? get() = if (reduced) null else SHIMMER

    /** Emphasized decelerate easing. */
    val decelerate: Easing get() = EmphasizedDecelerate

    /** Easing for exits. */
    val accelerate: Easing get() = EmphasizedAccelerate

    /** Easing for press feedback. */
    val press: Easing get() = LinearOutSlowInEasing

    private companion object {
        const val INSTANT = 90
        const val QUICK = 160
        const val STANDARD = 220
        const val EXIT = 140
        const val SHIMMER = 1200
        val EmphasizedDecelerate = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    }
}
