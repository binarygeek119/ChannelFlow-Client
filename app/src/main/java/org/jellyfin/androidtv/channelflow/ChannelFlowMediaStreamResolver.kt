package org.jellyfin.androidtv.channelflow

import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStreamContainer
import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.model.api.MediaType

class ChannelFlowMediaStreamResolver(
	private val catalog: ChannelFlowGuideRepository,
) : MediaStreamResolver {
	override suspend fun getStream(queueEntry: QueueEntry): PlayableMediaStream? {
		val item = queueEntry.baseItem ?: return null
		if (item.mediaType != MediaType.VIDEO) return null
		val url = catalog.getStreamUrl(item.id) ?: item.channelId?.let { catalog.getStreamUrl(it) } ?: return null

		return PlayableMediaStream(
			identifier = item.id.toString(),
			conversionMethod = MediaConversionMethod.None,
			container = MediaStreamContainer(format = ChannelFlowStream.container(url)),
			tracks = emptyList(),
			queueEntry = queueEntry,
			url = url,
		)
	}
}
