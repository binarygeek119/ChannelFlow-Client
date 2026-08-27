package org.jellyfin.androidtv.channelflow

import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Serializable
data class ChannelFlowReminder(
	val programId: String,
	val channelId: String,
	val title: String,
	val episodeTitle: String? = null,
	val channelLabel: String? = null,
	val startEpochMilli: Long,
	val endEpochMilli: Long,
) {
	val fireAtEpochMilli: Long
		get() = fireAt(startEpochMilli)

	fun isDue(nowEpochMilli: Long = System.currentTimeMillis()): Boolean =
		nowEpochMilli >= fireAtEpochMilli && !isExpired(nowEpochMilli)

	fun isExpired(nowEpochMilli: Long = System.currentTimeMillis()): Boolean =
		nowEpochMilli > endEpochMilli

	fun programUuid(): UUID = UUID.fromString(programId)
	fun channelUuid(): UUID = UUID.fromString(channelId)

	companion object {
		const val START_PADDING_SECONDS = 20L

		fun fireAt(startEpochMilli: Long, paddingSeconds: Long = START_PADDING_SECONDS): Long =
			startEpochMilli + paddingSeconds * 1_000L

		fun fromProgram(program: BaseItemDto, channelLabel: String?): ChannelFlowReminder? {
			val channelId = program.channelId ?: return null
			val start = program.startDate ?: return null
			val end = program.endDate ?: start.plusHours(3)
			if (!start.isAfter(LocalDateTime.now())) return null
			return ChannelFlowReminder(
				programId = program.id.toString(),
				channelId = channelId.toString(),
				title = program.name.orEmpty().ifBlank { "Program" },
				episodeTitle = program.episodeTitle?.takeIf { it.isNotBlank() },
				channelLabel = channelLabel?.takeIf { it.isNotBlank() },
				startEpochMilli = start.toEpochMilli(),
				endEpochMilli = end.toEpochMilli(),
			)
		}
	}
}

internal fun LocalDateTime.toEpochMilli(): Long =
	atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
