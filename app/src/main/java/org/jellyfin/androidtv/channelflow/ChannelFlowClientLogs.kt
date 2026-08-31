package org.jellyfin.androidtv.channelflow

object ChannelFlowClientLogs {
	const val PATH = "/api/client-logs"
	const val MAX_BATCH = 80
	const val MAX_QUEUE = 400
	const val MAX_MESSAGE = 8_000
	const val MAX_EXCEPTION = 16_000
	const val MAX_DEVICE_ID = 64

	fun ingestUrl(baseUrl: String): String =
		"${baseUrl.trimEnd('/')}$PATH"

	fun sanitizeDeviceId(raw: String?): String? {
		if (raw.isNullOrBlank()) return null
		val cleaned = raw.trim().filter { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
		if (cleaned.isEmpty()) return null
		return cleaned.take(MAX_DEVICE_ID)
	}

	fun sanitizeText(raw: String?, maxChars: Int): String? {
		if (raw.isNullOrBlank()) return null
		val trimmed = raw.trim()
		return if (trimmed.length <= maxChars) trimmed else trimmed.take(maxChars)
	}

	fun levelName(priority: Int): String = when (priority) {
		2 -> "verbose"
		3 -> "debug"
		5 -> "warn"
		6 -> "error"
		7 -> "assert"
		else -> "info"
	}
}
