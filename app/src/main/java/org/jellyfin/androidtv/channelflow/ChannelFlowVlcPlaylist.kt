package org.jellyfin.androidtv.channelflow

import java.io.File
import java.util.UUID

object ChannelFlowVlcPlaylist {
	fun text(
		streamUrl: String,
		name: String = "ChannelFlow",
		channelId: UUID? = null,
		number: String? = null,
		logoUrl: String? = null,
	): String {
		val title = escape(name.ifBlank { "ChannelFlow" })
		val extinf = buildString {
			append("#EXTINF:-1")
			if (channelId != null) append(" tvg-id=\"").append(channelId.toString().replace("-", "")).append('"')
			if (!number.isNullOrBlank()) append(" tvg-chno=\"").append(escape(number)).append('"')
			append(" tvg-name=\"").append(title).append('"')
			if (!logoUrl.isNullOrBlank()) append(" tvg-logo=\"").append(escape(logoUrl)).append('"')
			append(',').append(title)
		}
		return buildString {
			appendLine("#EXTM3U")
			appendLine(extinf)
			appendLine("#EXTVLCOPT:http-user-agent=ChannelFlow TV")
			appendLine("#EXTVLCOPT:network-caching=1500")
			appendLine("#EXTVLCOPT:live-caching=1500")
			appendLine("#EXTVLCOPT:http-reconnect=true")
			appendLine(streamUrl.trim())
		}
	}

	fun write(
		file: File,
		streamUrl: String,
		name: String = "ChannelFlow",
		channelId: UUID? = null,
		number: String? = null,
		logoUrl: String? = null,
	): File {
		file.parentFile?.mkdirs()
		file.writeText(text(streamUrl, name, channelId, number, logoUrl), Charsets.UTF_8)
		return file
	}

	private fun escape(value: String): String =
		value.replace(',', ' ').replace('\n', ' ').replace('"', '\'')
}
