package org.jellyfin.androidtv.ui.playback

import android.app.Activity
import android.net.Uri
import android.view.View
import org.jellyfin.androidtv.channelflow.ChannelFlowStream
import org.jellyfin.androidtv.channelflow.ChannelFlowVlcPlaylist
import org.jellyfin.androidtv.preference.constant.ZoomMode
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import timber.log.Timber
import java.io.File
import java.util.UUID

class VlcVideoEngine(
	private val activity: Activity,
	private val videoLayout: VLCVideoLayout,
	private val helper: PlaybackOverlayFragmentHelper,
) {
	private val libVlc: LibVLC
	private val player: MediaPlayer
	private var notifier: PlaybackControllerNotifiable? = null
	private var attached = false
	private var liveStream = false
	private var lastPosition = 0L
	private var lastUrl: String? = null

	init {
		val options = arrayListOf(
			"--network-caching=1500",
			"--live-caching=1500",
			"--http-reconnect",
			"--http-user-agent=ChannelFlow TV",
			"--no-drop-late-frames",
			"--no-skip-frames",
			"--aout=opensles",
			"--audio-time-stretch",
		)
		libVlc = LibVLC(activity, options)
		player = MediaPlayer(libVlc)
		player.setEventListener { event ->
			activity.runOnUiThread { onEvent(event) }
		}
	}

	fun subscribe(notifier: PlaybackControllerNotifiable) {
		this.notifier = notifier
	}

	fun isPlaying(): Boolean = player.isPlaying

	fun getCurrentPosition(): Long {
		if (!player.isPlaying) return lastPosition
		val time = player.time.coerceAtLeast(0L)
		lastPosition = time
		return time
	}

	fun getDuration(): Long = player.length.coerceAtLeast(0L)

	fun isSeekable(): Boolean = !liveStream && player.isSeekable

	fun seekTo(position: Long): Long {
		if (!isSeekable()) return -1L
		player.time = position
		lastPosition = position
		return position
	}

	fun getPlaybackSpeed(): Float = player.rate

	fun setPlaybackSpeed(speed: Float) {
		if (speed < 0.25f) return
		player.rate = speed
	}

	fun setZoom(mode: ZoomMode) {
		player.videoScale = when (mode) {
			ZoomMode.FIT -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
			ZoomMode.AUTO_CROP -> MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
			ZoomMode.STRETCH -> MediaPlayer.ScaleType.SURFACE_FILL
		}
	}

	fun setMedia(
		url: String,
		live: Boolean,
		name: String? = null,
		channelId: UUID? = null,
		number: String? = null,
		logoUrl: String? = null,
	) {
		liveStream = live
		lastUrl = url
		attachIfNeeded()
		val playlist = ChannelFlowVlcPlaylist.write(
			file = File(File(activity.cacheDir, "vlc"), "channel.m3u"),
			streamUrl = url,
			name = name ?: "ChannelFlow",
			channelId = channelId,
			number = number,
			logoUrl = logoUrl,
		)
		val media = Media(libVlc, Uri.fromFile(playlist))
		media.setHWDecoderEnabled(true, false)
		media.addOption(":demux=m3u,any")
		media.addOption(":network-caching=1500")
		media.addOption(":http-reconnect")
		if (live) {
			media.addOption(":live-caching=1500")
			media.addOption(":clock-jitter=0")
			media.addOption(":clock-synchro=0")
		}
		player.media = media
		media.release()
		Timber.i("VLC playing M3U live=%s url=%s", live, ChannelFlowStream.redact(url))
	}

	fun start() {
		attachIfNeeded()
		player.play()
	}

	fun play() {
		player.play()
	}

	fun pause() {
		player.pause()
	}

	fun stop() {
		runCatching { player.stop() }
		helper.setScreensaverLock(false)
	}

	fun release() {
		notifier = null
		stop()
		if (attached) {
			runCatching { player.detachViews() }
			attached = false
		}
		runCatching { player.release() }
		runCatching { libVlc.release() }
	}

	fun surface(): View = videoLayout

	private fun attachIfNeeded() {
		if (attached) return
		player.attachViews(videoLayout, null, false, false)
		attached = true
	}

	private fun onEvent(event: MediaPlayer.Event) {
		when (event.type) {
			MediaPlayer.Event.Playing -> {
				notifier?.onPrepared()
				helper.setScreensaverLock(true)
			}
			MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped -> {
				helper.setScreensaverLock(false)
			}
			MediaPlayer.Event.EndReached -> {
				if (liveStream) {
					Timber.w("VLC live stream ended; restarting")
					val url = lastUrl
					if (url != null) {
						setMedia(url, true)
						player.play()
					}
				} else {
					notifier?.onCompletion()
				}
			}
			MediaPlayer.Event.EncounteredError -> {
				Timber.e("VLC playback error")
				notifier?.onError()
			}
			MediaPlayer.Event.TimeChanged -> {
				notifier?.onProgress()
			}
		}
	}
}
