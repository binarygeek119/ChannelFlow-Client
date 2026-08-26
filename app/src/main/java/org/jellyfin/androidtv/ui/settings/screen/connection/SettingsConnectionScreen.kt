package org.jellyfin.androidtv.ui.settings.screen.connection

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.channelflow.ChannelFlowConnectionStore
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.koin.compose.koinInject

@Composable
fun SettingsConnectionScreen() {
	val store = koinInject<ChannelFlowConnectionStore>()
	val context = LocalContext.current
	val connection = store.connection

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_connection)) },
				captionContent = { Text(stringResource(R.string.pref_connection_description)) },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_tv), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_server)) },
				captionContent = { Text(connection?.baseUrl ?: stringResource(R.string.lbl_bracket_unknown)) },
				onClick = {},
				modifier = Modifier.focusKey("server_url"),
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_next), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_change_server)) },
				captionContent = { Text(stringResource(R.string.lbl_change_server_help)) },
				onClick = {
					context.startActivity(
						Intent(context, StartupActivity::class.java).apply {
							putExtra(StartupActivity.EXTRA_RECONNECT, true)
							putExtra(StartupActivity.EXTRA_HIDE_SPLASH, true)
							addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
						}
					)
				},
				modifier = Modifier.focusKey("change_server"),
			)
		}
	}
}
