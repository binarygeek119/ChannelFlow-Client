package org.jellyfin.androidtv.ui.playback

import android.content.Context
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Utility class to launch the playback UI for an item.
 */
class PlaybackLauncher(
	private val videoQueueManager: VideoQueueManager,
	private val navigationRepository: NavigationRepository,
	private val userPreferences: UserPreferences,
) {
	private val BaseItemDto.supportsExternalPlayer
		get() = when (type) {
			BaseItemKind.TV_CHANNEL,
			BaseItemKind.PROGRAM,
			BaseItemKind.LIVE_TV_CHANNEL,
				-> true

			else -> false
		}

	@JvmOverloads
	fun launch(
		context: Context,
		items: List<BaseItemDto>,
		position: Int? = null,
		replace: Boolean = false,
		itemsPosition: Int = 0,
		shuffle: Boolean = false,
	) {
		if (items.any { it.mediaType == MediaType.AUDIO }) return

		val items = if (shuffle) items.shuffled() else items

		videoQueueManager.setCurrentVideoQueue(items.toList())
		videoQueueManager.setCurrentMediaPosition(itemsPosition)

		if (items.isEmpty()) return

		if (userPreferences[UserPreferences.useExternalPlayer] && items.all { it.supportsExternalPlayer }) {
			context.startActivity(ActivityDestinations.externalPlayer(context, position?.milliseconds ?: Duration.ZERO))
		} else if (userPreferences[UserPreferences.playbackRewriteVideoEnabled]) {
			navigationRepository.navigate(Destinations.videoPlayerNew(position), replace)
		} else {
			navigationRepository.navigate(Destinations.videoPlayer(position), replace)
		}
	}
}
