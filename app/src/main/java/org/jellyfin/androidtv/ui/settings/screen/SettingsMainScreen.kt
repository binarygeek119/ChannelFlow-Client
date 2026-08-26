package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn

@Composable
fun SettingsMainScreen() {
	val router = LocalRouter.current

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
				headingContent = { Text(stringResource(R.string.pref_connection)) },
				onClick = { router.push(Routes.CONNECTION) },
				modifier = Modifier.focusKey(Routes.CONNECTION),
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_adjust), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_customization)) },
				onClick = { router.push(Routes.CUSTOMIZATION) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION),
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
				leadingContent = { Icon(painterResource(R.drawable.ic_guide), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_live_tv_cat)) },
				onClick = { router.push(Routes.LIVETV_GUIDE_OPTIONS) },
				modifier = Modifier.focusKey(Routes.LIVETV_GUIDE_OPTIONS),
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_error), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_telemetry_category)) },
				onClick = { router.push(Routes.TELEMETRY) },
				modifier = Modifier.focusKey(Routes.TELEMETRY),
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
