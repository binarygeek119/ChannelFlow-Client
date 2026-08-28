package org.jellyfin.androidtv.channelflow

import java.time.LocalDateTime

object ChannelFlowGuideClock {
	@Volatile
	private var coverageStart: LocalDateTime? = null

	@Volatile
	private var coverageEnd: LocalDateTime? = null

	fun updateCoverage(programs: List<ChannelFlowProgram>) {
		if (programs.isEmpty()) {
			coverageStart = null
			coverageEnd = null
			return
		}
		coverageStart = programs.minOf { it.start }
		coverageEnd = programs.maxOf { it.end }
	}

	fun now(): LocalDateTime = effectiveNow(LocalDateTime.now(), coverageStart, coverageEnd)

	fun effectiveNow(
		deviceNow: LocalDateTime,
		earliestStart: LocalDateTime?,
		latestEnd: LocalDateTime?,
	): LocalDateTime {
		if (earliestStart == null || latestEnd == null) return deviceNow
		if (deviceNow.isBefore(earliestStart)) return earliestStart
		if (!deviceNow.isBefore(latestEnd)) {
			return latestEnd.minusMinutes(1).let { if (it.isBefore(earliestStart)) earliestStart else it }
		}
		return deviceNow
	}
}
