package org.jellyfin.androidtv.ui.playback

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.UUID

fun PlaybackController.getLiveTvChannel(
	id: UUID,
	callback: (channel: BaseItemDto) -> Unit,
) {
	val api by fragment.inject<ApiClient>()

	fragment.lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getChannel(id).content
			}
		}.onSuccess { channel ->
			callback(channel)
		}
	}
}

fun PlaybackController.disableDefaultSubtitles() {
	Timber.i("Disabling non-baked subtitles")
}

@JvmOverloads
fun PlaybackController.setSubtitleIndex(index: Int, force: Boolean = false) {
	Timber.i("Switching subtitles from index ${mCurrentOptions.subtitleStreamIndex} to $index")

	if (mCurrentOptions.subtitleStreamIndex == index && !force) return

	val videoQueueManager by fragment.inject<VideoQueueManager>()
	if (index == -1) {
		videoQueueManager.setLastPlayedSubtitleLanguageIsoCode("")
	} else {
		val stream = currentMediaSource.mediaStreams?.firstOrNull { it.type == MediaStreamType.SUBTITLE && it.index == index }
		videoQueueManager.setLastPlayedSubtitleLanguageIsoCode(stream?.language)
	}

	if (index == -1) {
		mCurrentOptions.subtitleStreamIndex = -1

		if (burningSubs) {
			Timber.i("Disabling subtitle baking")

			stop()
			burningSubs = false
			play(mCurrentPosition, -1)
		}
	} else if (burningSubs) {
		Timber.i("Restarting playback to disable subtitle baking")

		stop()
		burningSubs = false
		mCurrentOptions.subtitleStreamIndex = index
		play(mCurrentPosition, index)
	} else {
		val mediaSource = currentMediaSource
		val stream = mediaSource.mediaStreams?.firstOrNull { it.type == MediaStreamType.SUBTITLE && it.index == index }
		if (stream == null) {
			Timber.w("Failed to find correct media stream")
			return setSubtitleIndex(-1)
		}

		when {
			stream.deliveryMethod == SubtitleDeliveryMethod.ENCODE || shouldBurnInSubtitles(currentStreamInfo.playMethod) -> {
				Timber.i("Restarting playback for subtitle baking")

				stop()
				burningSubs = true
				mCurrentOptions.subtitleStreamIndex = index
				play(mCurrentPosition, index)
			}

			stream.deliveryMethod == SubtitleDeliveryMethod.EXTERNAL ||
				stream.deliveryMethod == SubtitleDeliveryMethod.EMBED ||
				stream.deliveryMethod == SubtitleDeliveryMethod.HLS -> {
				mCurrentOptions.subtitleStreamIndex = index
			}

			stream.deliveryMethod == SubtitleDeliveryMethod.DROP || stream.deliveryMethod == null -> {
				Timber.i("Dropping subtitles")
				setSubtitleIndex(-1)
			}
		}
	}
}

fun PlaybackController.applyMediaSegments(
	item: BaseItemDto,
	callback: () -> Unit,
) {
	fragment?.clearSkipOverlay()
	callback()
}
