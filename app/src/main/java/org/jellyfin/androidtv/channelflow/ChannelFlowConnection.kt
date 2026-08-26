package org.jellyfin.androidtv.channelflow

import kotlinx.serialization.Serializable

@Serializable
data class ChannelFlowConnection(
	val baseUrl: String,
	val m3uUrl: String,
	val epgUrl: String,
	val apiKey: String,
)
