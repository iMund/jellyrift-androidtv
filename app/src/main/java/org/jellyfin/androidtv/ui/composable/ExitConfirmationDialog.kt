package org.jellyfin.androidtv.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalShapes
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.jellyfin.design.Tokens

/**
 * Asks whether to leave the app. "Cancel" comes first so it gets the initial focus and a stray confirm press on the
 * remote does not close the app.
 */
@Composable
fun ExitConfirmationDialog(
	visible: Boolean,
	onDismissRequest: () -> Unit,
	onConfirm: () -> Unit,
) {
	DialogBase(
		visible = visible,
		onDismissRequest = onDismissRequest,
	) {
		Column(
			modifier = Modifier
				.clip(LocalShapes.current.large)
				.background(JellyfinTheme.colorScheme.surface)
				.width(480.dp)
				.padding(Tokens.Space.spaceMd),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(Tokens.Space.spaceMd),
		) {
			Text(
				text = stringResource(R.string.exit_app_title, stringResource(R.string.app_name)),
				style = JellyfinTheme.typography.listHeadline.copy(color = JellyfinTheme.colorScheme.listHeadline),
				textAlign = TextAlign.Center,
			)

			Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.spaceSm)) {
				Button(onClick = onDismissRequest) { Text(stringResource(R.string.lbl_cancel)) }
				Button(onClick = onConfirm) { Text(stringResource(R.string.lbl_exit)) }
			}
		}
	}
}
