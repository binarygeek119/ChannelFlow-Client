package org.jellyfin.androidtv.ui.playback

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository
import org.jellyfin.androidtv.channelflow.ChannelFlowStream
import org.jellyfin.androidtv.data.compat.StreamInfo
import org.jellyfin.androidtv.data.compat.VideoOptions
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.hlsSegmentApi
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaSourceType
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.PlayMethod
import timber.log.Timber

class PlaybackManager(
	private val api: ApiClient,
	private val catalog: ChannelFlowGuideRepository,
) {
	fun getVideoStreamInfo(
		lifecycleOwner: LifecycleOwner,
		options: VideoOptions,
		startTimeTicks: Long,
		callback: Response<StreamInfo>,
	) = lifecycleOwner.lifecycleScope.launch {
		val result = getVideoStreamInfoInternal(options)
		lifecycleOwner.withStarted {
			result.fold(
				onSuccess = { callback.onResponse(it) },
				onFailure = { callback.onError(Exception(it)) },
			)
		}
	}

	fun changeVideoStream(
		lifecycleOwner: LifecycleOwner,
		stream: StreamInfo,
		options: VideoOptions,
		startTimeTicks: Long,
		callback: Response<StreamInfo>
	) = lifecycleOwner.lifecycleScope.launch {
		if (stream.playSessionId != null && stream.playMethod != PlayMethod.DIRECT_PLAY) {
			withContext(Dispatchers.IO) {
				api.hlsSegmentApi.stopEncodingProcess(api.deviceInfo.id, stream.playSessionId)
			}
		}

		val result = getVideoStreamInfoInternal(options)
		lifecycleOwner.withStarted {
			result.fold(
				onSuccess = { callback.onResponse(it) },
				onFailure = { callback.onError(Exception(it)) },
			)
		}
	}

	private suspend fun getVideoStreamInfoInternal(
		options: VideoOptions,
	) = runCatching {
		val itemId = requireNotNull(options.itemId) { "Item id cannot be null" }
		val streamUrl = catalog.awaitStreamUrl(itemId)
			?: error("No playable stream for $itemId")
		Timber.i(
			"Resolved ChannelFlow stream item=%s mime=%s live=%s url=%s",
			itemId,
			ChannelFlowStream.mimeType(streamUrl),
			ChannelFlowStream.isLive(streamUrl),
			ChannelFlowStream.redact(streamUrl),
		)
		val container = ChannelFlowStream.container(streamUrl)
		val live = ChannelFlowStream.isLive(streamUrl)

		StreamInfo().apply {
			this.itemId = itemId
			playMethod = PlayMethod.DIRECT_PLAY
			this.container = container
			mediaUrl = streamUrl
			mediaSource = MediaSourceInfo(
				protocol = if (streamUrl.startsWith("http", ignoreCase = true)) MediaProtocol.HTTP else MediaProtocol.FILE,
				id = itemId.toString(),
				path = streamUrl,
				type = MediaSourceType.DEFAULT,
				container = container,
				isRemote = true,
				readAtNativeFramerate = false,
				ignoreDts = true,
				ignoreIndex = false,
				genPtsInput = false,
				supportsTranscoding = false,
				supportsDirectStream = false,
				supportsDirectPlay = true,
				isInfiniteStream = live,
				requiresOpening = false,
				requiresClosing = false,
				requiresLooping = false,
				supportsProbing = false,
				transcodingSubProtocol = if (ChannelFlowStream.isHls(streamUrl)) MediaStreamProtocol.HLS else MediaStreamProtocol.HTTP,
				hasSegments = ChannelFlowStream.isHls(streamUrl),
				mediaStreams = emptyList(),
			)
		}
	}
}
