package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl

/**
 * Live MPEG-TS over HTTP stalls if ExoPlayer pauses the socket once the buffer is "full".
 * ChannelFlow fans out a shared encoder; a paused reader causes dropped packets and a freeze.
 */
@OptIn(UnstableApi::class)
class LiveStreamLoadControl : DefaultLoadControl() {
	override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean = true

	@Deprecated("Deprecated in Java")
	override fun shouldContinueLoading(
		playbackPositionUs: Long,
		bufferedDurationUs: Long,
		playbackSpeed: Float,
	): Boolean = true
}
