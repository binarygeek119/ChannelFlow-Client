package org.jellyfin.androidtv.ui.livetv

import android.content.Context
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import timber.log.Timber

class LiveTvStartup(
	private val catalog: ChannelFlowGuideRepository,
	private val playbackLauncher: PlaybackLauncher,
) {
	suspend fun playRandomChannel(context: Context) {
		val channels = runCatching {
			catalog.getChannels()
		}.onFailure { error ->
			Timber.w(error, "Unable to load live TV channels")
		}.getOrNull().orEmpty()

		if (channels.isEmpty()) {
			Toast.makeText(context, R.string.lbl_no_items, Toast.LENGTH_LONG).show()
			return
		}

		val channel = channels.random()
		Timber.i("Tuning random live TV channel ${channel.name} (${channel.id})")
		playbackLauncher.launch(context, listOf(channel))
	}
}
