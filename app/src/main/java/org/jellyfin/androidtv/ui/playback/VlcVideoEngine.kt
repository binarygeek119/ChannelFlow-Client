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
import org.videolan.libvlc.interfaces.IMedia
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
	private var lastApiKey: String? = null
	private var retriedSoft = false

	init {
		val options = arrayListOf(
			"--network-caching=${ChannelFlowVlcPlaylist.START_CACHING_MS}",
			"--live-caching=${ChannelFlowVlcPlaylist.START_CACHING_MS}",
			"--prefetch-buffer-size=${ChannelFlowVlcPlaylist.PREFETCH_BUFFER_KIB}",
			"--prefetch-read-size=${ChannelFlowVlcPlaylist.PREFETCH_READ_SIZE}",
			"--http-reconnect",
			"--http-user-agent=${ChannelFlowVlcPlaylist.USER_AGENT}",
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
		apiKey: String? = null,
	) {
		liveStream = live
		lastUrl = url
		lastApiKey = apiKey
		retriedSoft = false
		attachIfNeeded()
		val playlist = ChannelFlowVlcPlaylist.write(
			file = File(File(activity.cacheDir, "vlc"), "channel.m3u"),
			streamUrl = url,
			name = name ?: "ChannelFlow",
			channelId = channelId,
			number = number,
			logoUrl = logoUrl,
			apiKey = apiKey,
		)
		val playUrl = ChannelFlowVlcPlaylist.streamUrlFrom(playlist.readText())
			?: ChannelFlowVlcPlaylist.withApiKey(url, apiKey)
		val media = mediaFromPlaylist(playlist) ?: mediaFromUrl(playUrl, preferHardware = true)
		player.media = media
		media.release()
		Timber.i("VLC playing M3U live=%s url=%s", live, ChannelFlowStream.redact(playUrl))
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

	private fun mediaFromPlaylist(file: File): Media? {
		val playlist = Media(libVlc, file.absolutePath)
		var stream: Media? = null
		try {
			if (!playlist.parse(IMedia.Parse.ParseLocal)) return null
			val items = playlist.subItems()
			try {
				if (items.count <= 0) return null
				stream = items.getMediaAt(0) as? Media ?: return null
				applyStreamOptions(stream, preferHardware = true)
				Timber.i("VLC opened M3U subitem uri=%s", stream.uri)
			} finally {
				items.release()
			}
		} catch (error: Throwable) {
			Timber.w(error, "VLC could not parse M3U playlist")
			stream = null
		} finally {
			playlist.release()
		}
		return stream
	}

	private fun mediaFromUrl(url: String, preferHardware: Boolean): Media {
		val media = Media(libVlc, Uri.parse(url))
		applyStreamOptions(media, preferHardware)
		return media
	}

	private fun applyStreamOptions(media: Media, preferHardware: Boolean) {
		media.setHWDecoderEnabled(preferHardware, true)
		media.addOption(":network-caching=${ChannelFlowVlcPlaylist.START_CACHING_MS}")
		media.addOption(":prefetch-buffer-size=${ChannelFlowVlcPlaylist.PREFETCH_BUFFER_KIB}")
		media.addOption(":prefetch-read-size=${ChannelFlowVlcPlaylist.PREFETCH_READ_SIZE}")
		media.addOption(":http-reconnect")
		media.addOption(":http-user-agent=${ChannelFlowVlcPlaylist.USER_AGENT}")
		val apiKey = lastApiKey
		if (!apiKey.isNullOrBlank()) media.addOption(":http-header=X-Api-Key: $apiKey")
		if (liveStream) {
			media.addOption(":live-caching=${ChannelFlowVlcPlaylist.START_CACHING_MS}")
			media.addOption(":clock-jitter=0")
			media.addOption(":clock-synchro=0")
		}
		val url = media.uri?.toString() ?: lastUrl.orEmpty()
		if (ChannelFlowStream.isMpegTs(url) && !ChannelFlowStream.isHls(url)) {
			media.addOption(":demux=ts")
		}
	}

	private fun attachIfNeeded() {
		if (attached) return
		videoLayout.visibility = View.VISIBLE
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
					restart(preferHardware = true)
				} else {
					notifier?.onCompletion()
				}
			}
			MediaPlayer.Event.EncounteredError -> {
				val url = lastUrl
				if (!retriedSoft && url != null) {
					retriedSoft = true
					Timber.w("VLC playback error; retrying stream without hardware decode")
					restart(preferHardware = false)
				} else {
					Timber.e("VLC playback error")
					notifier?.onError()
				}
			}
			MediaPlayer.Event.TimeChanged -> {
				notifier?.onProgress()
			}
		}
	}

	private fun restart(preferHardware: Boolean) {
		val url = lastUrl ?: return
		val playUrl = ChannelFlowVlcPlaylist.withApiKey(url, lastApiKey)
		val media = mediaFromUrl(playUrl, preferHardware)
		player.media = media
		media.release()
		player.play()
	}
}
