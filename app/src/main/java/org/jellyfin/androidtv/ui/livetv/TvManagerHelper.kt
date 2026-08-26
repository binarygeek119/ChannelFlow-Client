package org.jellyfin.androidtv.ui.livetv

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.android.ext.android.inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun BaseItemDto.copyWithLastPlayedDate(
	lastPlayedDate: LocalDateTime,
) = copy(
	userData = userData?.copy(
		lastPlayedDate = lastPlayedDate,
	)
)

fun loadLiveTvChannels(fragment: Fragment, callback: (channels: Collection<BaseItemDto>?) -> Unit) {
	val catalog by fragment.inject<ChannelFlowGuideRepository>()

	fragment.lifecycleScope.launch {
		runCatching {
			catalog.getChannels()
		}.fold(
			onSuccess = { channels -> callback(channels) },
			onFailure = { callback(null) },
		)
	}
}

fun getPrograms(
	fragment: Fragment,
	channelIds: Array<UUID>,
	startTime: LocalDateTime,
	endTime: LocalDateTime,
	callback: (programs: Collection<BaseItemDto>?) -> Unit,
) {
	val catalog by fragment.inject<ChannelFlowGuideRepository>()

	fragment.lifecycleScope.launch {
		runCatching {
			catalog.getPrograms(channelIds.toList(), startTime, endTime)
		}.fold(
			onSuccess = { programs -> callback(programs) },
			onFailure = { callback(null) },
		)
	}
}

fun getScheduleRows(
	fragment: Fragment,
	seriesTimerId: String?,
	callback: (timers: Map<LocalDate, List<BaseItemDto>>?) -> Unit,
) {
	callback(emptyMap())
}
