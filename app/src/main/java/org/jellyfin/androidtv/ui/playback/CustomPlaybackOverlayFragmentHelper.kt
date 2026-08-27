package org.jellyfin.androidtv.ui.playback

import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.ui.GuideChannelHeader
import org.jellyfin.androidtv.ui.livetv.TvManager
import org.jellyfin.androidtv.ui.livetv.adjacentChannelByNumber
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

private var lastChannelChangeAt = 0L
private const val CHANNEL_CHANGE_DEBOUNCE_MS = 250L

fun CustomPlaybackOverlayFragment.toggleFavorite() {
	val header = mSelectedProgramView as? GuideChannelHeader
	val channel = header?.channel ?: return

	val itemMutationRepository by inject<ItemMutationRepository>()
	val dataRefreshService by inject<DataRefreshService>()

	lifecycleScope.launch {
		runCatching {
			val userData = itemMutationRepository.setFavorite(
				item = header.channel.id,
				favorite = !(channel.userData?.isFavorite ?: false)
			)

			header.channel = header.channel.copy(userData = userData)
			header.findViewById<View>(R.id.favImage).isVisible = userData.isFavorite
			dataRefreshService.lastFavoriteUpdate = Instant.now()
		}
	}
}

fun CustomPlaybackOverlayFragment.refreshSelectedProgram() {
	val catalog by inject<ChannelFlowGuideRepository>()

	lifecycleScope.launch {
		runCatching {
			catalog.getProgram(mSelectedProgram.id)
		}.onSuccess { item ->
			if (item != null) mSelectedProgram = item
		}.onFailure { error ->
			Timber.e(error, "Unable to get program details")
		}

		detailUpdateInternal();
	}
}

fun CustomPlaybackOverlayFragment.changeChannelByNumber(higher: Boolean) {
	val now = android.os.SystemClock.elapsedRealtime()
	if (now - lastChannelChangeAt < CHANNEL_CHANGE_DEBOUNCE_MS) return
	lastChannelChangeAt = now

	val playbackControllerContainer by inject<PlaybackControllerContainer>()
	val current = playbackControllerContainer.playbackController?.currentlyPlayingItem ?: return
	val currentId = if (current.type == BaseItemKind.TV_CHANNEL) current.id else current.channelId ?: current.id

	fun tune(channels: Collection<org.jellyfin.sdk.model.api.BaseItemDto>?) {
		val next = adjacentChannelByNumber(channels, currentId, higher) ?: return
		if (next.id == currentId) return
		Timber.i("Changing live channel %s to %s (%s)", if (higher) "up" else "down", next.number, next.name)
		switchChannel(next.id)
	}

	val loaded = TvManager.getAllChannels()
	if (loaded.isNullOrEmpty()) {
		TvManager.loadAllChannels(this) {
			tune(TvManager.getAllChannels())
			null
		}
	} else {
		tune(loaded)
	}
}

fun CustomPlaybackOverlayFragment.playChannel(id: UUID) {
	val catalog by inject<ChannelFlowGuideRepository>()
	val playbackControllerContainer by inject<PlaybackControllerContainer>()

	lifecycleScope.launch {
		runCatching {
			catalog.getChannel(id)
		}.fold(
			onSuccess = { channel ->
				if (channel == null) {
					Toast.makeText(
						requireContext(),
						getString(R.string.msg_video_playback_error),
						Toast.LENGTH_LONG
					).show()
					closePlayer()
					return@fold
				}
				playbackControllerContainer.playbackController?.setItems(listOf(channel))
				playbackControllerContainer.playbackController?.play(0)
			},
			onFailure = {
				Toast.makeText(
					requireContext(),
					getString(R.string.msg_video_playback_error),
					Toast.LENGTH_LONG
				).show()

				closePlayer()
			},
		)
	}
}

fun CustomPlaybackOverlayFragment.askToSkip(position: Duration) {
	binding.skipOverlay.targetPosition = position
}
