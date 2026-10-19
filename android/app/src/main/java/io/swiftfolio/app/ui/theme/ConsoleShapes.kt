package io.swiftfolio.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Shape scale (DIRECTION-C.md 4.1): rows 10, fields 14, bars/sheets 20, pills full. */
val ConsoleShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** 8dp squircle used by file-type tiles. */
val TileShape = RoundedCornerShape(8.dp)

/** 4dp spacing grid and fixed component sizes. */
object Spacing {
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 20.dp
    val space6 = 24.dp
    val space8 = 32.dp
    val space12 = 48.dp

    /** Horizontal screen gutter. */
    val gutter = space4

    /** Minimum interactive size (accessibility). */
    val touchTarget = 48.dp

    /** Folder rows / file rows minimum heights (borrowed from Colophon). */
    val folderRowMinHeight = 56.dp
    val fileRowMinHeight = 64.dp

    /** File-type tile size. */
    val tileSize = 36.dp

    /** Floating command bar height and outer margin. */
    val commandBarHeight = 64.dp
    val commandBarMargin = 12.dp

    /** Path pill height. */
    val pathPillHeight = 40.dp

    /** Space reserved under the list so the last row clears the floating chrome. */
    val listBottomInset = commandBarHeight + pathPillHeight + space12

    /** Primary button height. */
    val primaryButtonHeight = 56.dp

    /** Hairline / rim thickness. */
    val hairline = 1.dp
}
