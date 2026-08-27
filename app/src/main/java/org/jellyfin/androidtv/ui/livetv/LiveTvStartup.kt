package org.jellyfin.androidtv.ui.livetv

import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository

class LiveTvStartup(
	private val catalog: ChannelFlowGuideRepository,
) {
	fun prefetchGuide() {
		catalog.prefetchLatest()
	}
}
