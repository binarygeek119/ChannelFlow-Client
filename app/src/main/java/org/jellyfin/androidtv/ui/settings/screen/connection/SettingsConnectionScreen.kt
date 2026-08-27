package org.jellyfin.androidtv.ui.settings.screen.connection

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.channelflow.ChannelFlowConnectionStore
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository
import org.jellyfin.androidtv.channelflow.reloadChannelFlowMain
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun SettingsConnectionScreen() {
	val store = koinInject<ChannelFlowConnectionStore>()
	val catalog = koinInject<ChannelFlowGuideRepository>()
	val settingsViewModel = koinActivityViewModel<SettingsViewModel>()
	val context = LocalContext.current
	val state by store.state.collectAsState()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.lbl_switch_server)) },
				captionContent = { Text(stringResource(R.string.lbl_switch_server_help)) },
			)
		}

		if (state.servers.isEmpty()) {
			item {
				ListButton(
					headingContent = { Text(stringResource(R.string.lbl_no_saved_servers)) },
					onClick = {},
					enabled = false,
					modifier = Modifier.focusKey("no_servers"),
				)
			}
		} else {
			items(state.servers, key = { it.id }) { server ->
				val selected = server.id == state.activeServerId
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_tv), contentDescription = null) },
					headingContent = { Text(server.connection.displayName()) },
					captionContent = { Text(server.connection.baseUrl) },
					trailingContent = { RadioButton(checked = selected) },
					onClick = {
						if (selected) return@ListButton
						if (!store.setActive(server.id)) return@ListButton
						catalog.clear()
						settingsViewModel.hide()
						context.reloadChannelFlowMain()
					},
					modifier = Modifier.focusKey("server_${server.id}"),
				)
			}
		}
	}
}
