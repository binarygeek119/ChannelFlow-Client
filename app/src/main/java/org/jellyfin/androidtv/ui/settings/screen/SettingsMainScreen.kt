package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.channelflow.ChannelFlowConnectionStore
import org.jellyfin.androidtv.channelflow.startChannelFlowPairing
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun SettingsMainScreen() {
	val router = LocalRouter.current
	val context = LocalContext.current
	val store = koinInject<ChannelFlowConnectionStore>()
	val settingsViewModel = koinActivityViewModel<SettingsViewModel>()
	val state by store.state.collectAsState()
	val currentName = state.connection?.displayName() ?: stringResource(R.string.lbl_bracket_unknown)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.app_name).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings)) },
				captionContent = { Text(stringResource(R.string.settings_description)) },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_tv), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_switch_server)) },
				captionContent = { Text(currentName) },
				onClick = { router.push(Routes.CONNECTION) },
				modifier = Modifier.focusKey(Routes.CONNECTION),
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_add_server)) },
				captionContent = { Text(stringResource(R.string.lbl_add_server_help)) },
				onClick = {
					settingsViewModel.hide()
					context.startChannelFlowPairing()
				},
				modifier = Modifier.focusKey("add_server"),
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_remove_server)) },
				captionContent = { Text(stringResource(R.string.lbl_remove_server_help)) },
				onClick = { router.push(Routes.CONNECTION_REMOVE) },
				modifier = Modifier.focusKey(Routes.CONNECTION_REMOVE),
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_next), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback)) },
				onClick = { router.push(Routes.PLAYBACK) },
				modifier = Modifier.focusKey(Routes.PLAYBACK),
			)
		}

		item {
			ListButton(
				leadingContent = {
					Icon(
						painter = painterResource(R.drawable.app_icon_foreground),
						contentDescription = null,
						tint = Color.Unspecified,
					)
				},
				headingContent = { Text(stringResource(R.string.pref_about_title)) },
				onClick = { router.push(Routes.ABOUT) },
				modifier = Modifier.focusKey(Routes.ABOUT),
			)
		}
	}
}
