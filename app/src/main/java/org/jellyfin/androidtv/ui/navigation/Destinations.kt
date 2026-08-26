package org.jellyfin.androidtv.ui.navigation

import org.jellyfin.androidtv.ui.livetv.LiveTvGuideFragment
import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment
import org.jellyfin.androidtv.ui.player.video.VideoPlayerFragment

object Destinations {
	val liveTvGuide = fragmentDestination<LiveTvGuideFragment>()

	fun videoPlayer(position: Int?) = fragmentDestination<CustomPlaybackOverlayFragment> {
		putInt("Position", position ?: 0)
	}

	fun videoPlayerNew(position: Int?) = fragmentDestination<VideoPlayerFragment> {
		putInt(VideoPlayerFragment.EXTRA_POSITION, position ?: 0)
	}
}
