package org.jellyfin.androidtv.channelflow

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class ChannelFlowConnection(
	val baseUrl: String,
	val m3uUrl: String,
	val epgUrl: String,
	val apiKey: String,
) {
	fun displayName(): String {
		val uri = runCatching { Uri.parse(baseUrl) }.getOrNull() ?: return baseUrl
		val host = uri.host?.removePrefix("www.").orEmpty()
		if (host.isBlank()) return baseUrl
		val port = uri.port
		return if (port != -1) "$host:$port" else host
	}
}

@Serializable
data class ChannelFlowSavedServer(
	val id: String,
	val connection: ChannelFlowConnection,
)
