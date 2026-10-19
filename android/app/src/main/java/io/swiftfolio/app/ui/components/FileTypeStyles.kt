package io.swiftfolio.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.theme.ConsoleColors
import io.swiftfolio.core.FileKind

/**
 * Visual identity of a file family: icon, tint per theme and an accessible label, so the
 * type is never conveyed by colour alone (borrowed from Squishy Bucket).
 */
@Immutable
data class TileStyle(
    val icon: ImageVector,
    val darkTint: Color,
    val lightTint: Color,
    @StringRes val labelRes: Int,
) {
    /** Tint for the current theme. */
    fun tint(colors: ConsoleColors): Color = if (colors.isDark) darkTint else lightTint
}

/** The table that maps every [FileKind] to its [TileStyle]. Add a kind here only. */
object FileTypeStyles {
    private val styles: Map<FileKind, TileStyle> = mapOf(
        FileKind.IMAGE to TileStyle(Icons.Rounded.Image, Color(0xFFF472B6), Color(0xFFBE185D), R.string.kind_image),
        FileKind.VIDEO to TileStyle(Icons.Rounded.Movie, Color(0xFFA78BFA), Color(0xFF6D28D9), R.string.kind_video),
        FileKind.AUDIO to TileStyle(Icons.Rounded.AudioFile, Color(0xFF34D399), Color(0xFF047857), R.string.kind_audio),
        FileKind.PDF to TileStyle(Icons.Rounded.PictureAsPdf, Color(0xFFF87171), Color(0xFFB91C1C), R.string.kind_pdf),
        FileKind.DOCUMENT to TileStyle(Icons.Rounded.TableChart, Color(0xFF60A5FA), Color(0xFF1D4ED8), R.string.kind_document),
        FileKind.ARCHIVE to TileStyle(Icons.Rounded.FolderZip, Color(0xFFFBBF24), Color(0xFFB45309), R.string.kind_archive),
        FileKind.CODE to TileStyle(Icons.Rounded.DataObject, Color(0xFF22D3EE), Color(0xFF0E7490), R.string.kind_code),
        FileKind.TEXT to TileStyle(Icons.AutoMirrored.Rounded.TextSnippet, Color(0xFFCBD5E1), Color(0xFF475569), R.string.kind_text),
        FileKind.OTHER to TileStyle(Icons.AutoMirrored.Rounded.InsertDriveFile, Color(0xFF8590A6), Color(0xFF5A647A), R.string.kind_other),
    )

    /** Folders use the accent text colour of the theme. */
    val folder: TileStyle = TileStyle(Icons.Rounded.Folder, Color(0xFF7FA8FF), Color(0xFF2F5BEA), R.string.kind_folder)

    /** Style for [kind]; every kind has an entry. */
    fun of(kind: FileKind): TileStyle = styles.getValue(kind)
}
