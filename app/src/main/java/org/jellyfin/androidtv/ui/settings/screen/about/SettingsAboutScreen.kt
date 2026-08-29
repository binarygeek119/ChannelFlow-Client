package org.jellyfin.androidtv.ui.settings.screen.about

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.channelflow.ChannelFlowUpdateChecker
import org.jellyfin.androidtv.channelflow.ChannelFlowUpdateStatus
import org.jellyfin.androidtv.channelflow.ChannelFlowVersion
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.util.copyAction
import org.koin.compose.koinInject

@Composable
fun SettingsAboutScreen(launchedFromLogin: Boolean = false) {
	val router = LocalRouter.current
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val updater = koinInject<ChannelFlowUpdateChecker>()
	val updateStatus by updater.status.collectAsState()

	LaunchedEffect(Unit) {
		updater.check()
	}

	SettingsColumn {
		if (launchedFromLogin) item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_login).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_about_title)) },
			)
		} else item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_about_title)) },
			)
		}

		item {
			val heading = "Version"
			val caption = ChannelFlowVersion.display(BuildConfig.VERSION_NAME)
			ListButton(
				leadingContent = {
					Icon(
						painter = painterResource(R.drawable.app_icon_foreground),
						contentDescription = null,
						tint = Color.Unspecified,
					)
				},
				headingContent = { Text(heading) },
				captionContent = { Text(caption) },
				onClick = copyAction(ClipData.newPlainText(heading, caption)),
				modifier = Modifier.focusKey("version")
			)
		}

		item {
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
				modifier = Modifier.focusKey("updates")
			)
		}

		item {
			val heading = stringResource(R.string.license_author)
			val caption = "binarygeek119"
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_user), contentDescription = null) },
				headingContent = { Text(heading) },
				captionContent = { Text(caption) },
				onClick = copyAction(ClipData.newPlainText(heading, caption)),
				modifier = Modifier.focusKey("author")
			)
		}

		item {
			val heading = stringResource(R.string.pref_device_model)
			val caption = "${Build.MANUFACTURER} ${Build.MODEL}"
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_tv), contentDescription = null) },
				headingContent = { Text(heading) },
				captionContent = { Text(caption) },
				onClick = copyAction(ClipData.newPlainText(heading, caption)),
				modifier = Modifier.focusKey("device_model")
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_guide), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.licenses_link)) },
				onClick = { router.push(Routes.LICENSES) },
				modifier = Modifier.focusKey(Routes.LICENSES)
			)
		}

		if (!launchedFromLogin) item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_flask), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_developer_link)) },
				onClick = { router.push(Routes.DEVELOPER) },
				modifier = Modifier.focusKey(Routes.DEVELOPER)
			)
		}
	}
}
