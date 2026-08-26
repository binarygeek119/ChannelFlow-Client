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
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

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
