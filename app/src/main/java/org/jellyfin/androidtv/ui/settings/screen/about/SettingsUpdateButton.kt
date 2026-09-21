package org.jellyfin.androidtv.ui.settings.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.update.ApkInstaller
import org.jellyfin.androidtv.update.AppUpdater
import org.jellyfin.androidtv.update.ReleaseInfo
import org.jellyfin.androidtv.update.UpdateCheck
import org.koin.compose.koinInject

private sealed interface UpdateUiState {
	data object Idle : UpdateUiState
	data object Checking : UpdateUiState
	data object UpToDate : UpdateUiState
	data object CheckFailed : UpdateUiState
	data class Available(val release: ReleaseInfo) : UpdateUiState
	data class Downloading(val release: ReleaseInfo, val percent: Int) : UpdateUiState
	data class DownloadFailed(val release: ReleaseInfo) : UpdateUiState
	data class NeedsPermission(val release: ReleaseInfo, val settingsAvailable: Boolean) : UpdateUiState
	data class InstallFailed(val release: ReleaseInfo) : UpdateUiState
	data object WaitingForConfirmation : UpdateUiState
}

/** Checks for a newer release of the app, downloads it and hands it to the system installer. */
@Composable
fun SettingsUpdateButton(
	updater: AppUpdater = koinInject(),
	installer: ApkInstaller = koinInject(),
) {
	if (!updater.enabled) return

	val scope = rememberCoroutineScope()
	var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }

	fun install(release: ReleaseInfo) {
		if (!installer.allowed) {
			state = UpdateUiState.NeedsPermission(release, installer.openPermissionSettings())
			return
		}

		scope.launch {
			state = UpdateUiState.Downloading(release, 0)
			val apk = updater.download(release) { progress ->
				state = UpdateUiState.Downloading(release, (progress * 100).toInt())
			}

			state = when {
				apk == null -> UpdateUiState.DownloadFailed(release)
				!installer.install(apk) -> UpdateUiState.InstallFailed(release)
				else -> UpdateUiState.WaitingForConfirmation
			}
		}
	}

	fun check() {
		scope.launch {
			state = UpdateUiState.Checking
			state = when (val result = updater.check()) {
				UpdateCheck.UpToDate -> UpdateUiState.UpToDate
				UpdateCheck.Failed -> UpdateUiState.CheckFailed
				is UpdateCheck.Available -> UpdateUiState.Available(result.release)
			}
		}
	}

	val current = state
	val busy = current is UpdateUiState.Checking || current is UpdateUiState.Downloading
	val caption = when (current) {
		UpdateUiState.Idle -> null
		UpdateUiState.Checking -> stringResource(R.string.update_checking)
		UpdateUiState.UpToDate -> stringResource(R.string.update_up_to_date)
		UpdateUiState.CheckFailed -> stringResource(R.string.update_check_failed)
		is UpdateUiState.Available -> stringResource(R.string.update_available, current.release.tag.removePrefix("v"))
		is UpdateUiState.Downloading -> stringResource(R.string.update_downloading, current.percent)
		is UpdateUiState.DownloadFailed -> stringResource(R.string.update_download_failed)
		is UpdateUiState.NeedsPermission -> stringResource(
			if (current.settingsAvailable) R.string.update_permission_needed else R.string.update_permission_unavailable
		)
		is UpdateUiState.InstallFailed -> stringResource(R.string.update_install_failed)
		UpdateUiState.WaitingForConfirmation -> stringResource(R.string.update_confirm_install)
	}

	ListButton(
		leadingContent = { Icon(painterResource(R.drawable.ic_upload), contentDescription = null) },
		headingContent = { Text(stringResource(R.string.update_title)) },
		captionContent = caption?.let { text -> { Text(text) } },
		enabled = !busy,
		onClick = {
			when (current) {
				is UpdateUiState.Available -> install(current.release)
				is UpdateUiState.DownloadFailed -> install(current.release)
				is UpdateUiState.NeedsPermission -> install(current.release)
				is UpdateUiState.InstallFailed -> install(current.release)
				else -> check()
			}
		},
		modifier = Modifier.focusKey("update"),
	)
}
