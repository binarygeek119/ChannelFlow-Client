package org.jellyfin.androidtv.channelflow

import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import java.util.UUID

object ChannelFlowIds {
	fun parse(raw: String?): UUID? {
		if (raw.isNullOrBlank()) return null
		raw.toUUIDOrNull()?.let { return it }

		val hex = raw.filter { it.isLetterOrDigit() }
		if (hex.length != 32) return null
		return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
			.toUUIDOrNull()
	}
}
