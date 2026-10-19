package io.swiftfolio.app.ui.browser.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.ErrorTexts
import io.swiftfolio.app.ui.browser.BrowserAction
import io.swiftfolio.app.ui.browser.BrowserSheet
import io.swiftfolio.app.ui.components.ConsoleTextField
import io.swiftfolio.app.ui.components.GradientButton
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.core.FieldError
import io.swiftfolio.core.FolderNameValidator
import io.swiftfolio.core.S3Path

/** Keyboard-first create-folder sheet with a live `s3://…/<name>/` preview. */
@Composable
internal fun NewFolderSheet(
    sheet: BrowserSheet.NewFolder,
    bucket: String,
    prefix: String,
    validationError: FieldError?,
    onAction: (BrowserAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ConsoleTheme.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val canCreate = sheet.name.isNotBlank() && validationError == null && !sheet.isCreating
    ConsoleSheet(onDismiss = onDismiss, title = stringResource(R.string.new_folder)) {
        Text(
            text = buildAnnotatedString {
                append(S3Path.s3Uri(bucket, prefix))
                withStyle(SpanStyle(color = colors.accentText)) { append(sheet.name.trim() + "/") }
            },
            style = ConsoleTheme.mono.meta,
            color = colors.textTertiary,
            modifier = Modifier.padding(horizontal = Spacing.space6),
        )
        ConsoleTextField(
            value = sheet.name,
            onValueChange = { onAction(BrowserAction.NewFolderNameChanged(it)) },
            label = stringResource(R.string.folder_name),
            enabled = !sheet.isCreating,
            errorText = validationError?.let { ErrorTexts.field(it, ErrorTexts.LengthLimits.FOLDER) } ?: sheet.error?.let { ErrorTexts.title(it) },
            helperText = if (FolderNameValidator.hasOuterWhitespace(sheet.name)) stringResource(R.string.folder_name_trimmed) else null,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (canCreate) onAction(BrowserAction.CreateFolder) }),
            modifier = Modifier.padding(horizontal = Spacing.space6, vertical = Spacing.space3).focusRequester(focusRequester),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space6),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space2, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            GradientButton(
                text = stringResource(R.string.create),
                onClick = { onAction(BrowserAction.CreateFolder) },
                enabled = canCreate,
                loading = sheet.isCreating,
                minHeight = Spacing.touchTarget,
            )
        }
    }
}
