package org.jellyfin.androidtv.ui.settings.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.channelflow.ChannelFlowUpdateChecker
import org.jellyfin.androidtv.channelflow.ChannelFlowUpdateStatus
import org.jellyfin.androidtv.channelflow.ChannelFlowVersion
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun ChannelFlowUpdateSettingsButton() {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val updater = koinInject<ChannelFlowUpdateChecker>()
	val settingsViewModel = koinActivityViewModel<SettingsViewModel>()
	val updateStatus by updater.status.collectAsState()

	LaunchedEffect(Unit) {
		updater.check()
	}

	val caption = when (val status = updateStatus) {
		is ChannelFlowUpdateStatus.Checking, ChannelFlowUpdateStatus.Idle ->
			stringResource(R.string.lbl_checking_for_updates)
		is ChannelFlowUpdateStatus.UpToDate ->
			stringResource(R.string.lbl_app_up_to_date, ChannelFlowVersion.display(status.latest))
		is ChannelFlowUpdateStatus.Available -> if (status.apkUrl.isNullOrBlank()) {
			stringResource(R.string.lbl_update_no_apk)
		} else {
			stringResource(R.string.lbl_update_available, ChannelFlowVersion.display(status.latest))
		}
		is ChannelFlowUpdateStatus.Downloading ->
			stringResource(R.string.lbl_downloading_update, status.progress)
		is ChannelFlowUpdateStatus.Installing ->
			stringResource(R.string.lbl_installing_update)
		is ChannelFlowUpdateStatus.Failed ->
			status.reason?.takeIf { it.isNotBlank() }
				?: stringResource(R.string.lbl_update_check_failed)
	}
	val heading = when (val status = updateStatus) {
		is ChannelFlowUpdateStatus.Available -> if (status.apkUrl.isNullOrBlank()) {
			stringResource(R.string.lbl_open_github_release)
		} else if (updater.needsInstallPermission()) {
			stringResource(R.string.lbl_allow_unknown_sources)
		} else {
			stringResource(R.string.lbl_install_update)
		}
		is ChannelFlowUpdateStatus.Downloading ->
			stringResource(R.string.lbl_downloading_update, status.progress)
		is ChannelFlowUpdateStatus.Installing ->
			stringResource(R.string.lbl_installing_update)
		else -> stringResource(R.string.lbl_check_for_updates)
	}

	ListButton(
		leadingContent = { Icon(painterResource(R.drawable.ic_refresh), contentDescription = null) },
		headingContent = { Text(heading) },
		captionContent = {
			val color = when (updateStatus) {
				is ChannelFlowUpdateStatus.Available,
				is ChannelFlowUpdateStatus.Downloading,
				is ChannelFlowUpdateStatus.Installing,
					-> JellyfinTheme.colorScheme.recording
				else -> JellyfinTheme.colorScheme.listCaption
			}
			Text(caption, color = color)
		},
		onClick = {
			when (val status = updateStatus) {
				is ChannelFlowUpdateStatus.Downloading,
				is ChannelFlowUpdateStatus.Installing,
					-> Unit
				is ChannelFlowUpdateStatus.Available -> {
					if (status.apkUrl.isNullOrBlank()) {
						runCatching {
							context.startActivity(
								Intent(Intent.ACTION_VIEW, Uri.parse(status.pageUrl))
									.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
							)
						}
					} else if (updater.needsInstallPermission()) {
						runCatching { context.startActivity(updater.installPermissionIntent()) }
					} else {
						settingsViewModel.hide()
						updater.startInstall(context)
					}
				}
				is ChannelFlowUpdateStatus.Failed -> {
					if (!updater.retryInstall(context)) {
						scope.launch { updater.check(force = true) }
					}
				}
				else -> scope.launch { updater.check(force = true) }
			}
		},
		modifier = Modifier.focusKey("updates"),
	)
}
