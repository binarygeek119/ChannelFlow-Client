package org.jellyfin.androidtv.channelflow

import android.net.Uri

object ChannelFlowUrls {
	fun normalizeServerUrl(input: String): String? {
		var value = input.trim()
		if (value.isBlank()) return null
		if (!value.contains("://")) value = "https://$value"

		val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
		if (uri.host.isNullOrBlank()) return null

		val path = uri.path.orEmpty().trimEnd('/')
		val cleanedPath = when {
			path.equals("/pair", ignoreCase = true) -> ""
			path.endsWith("/pair.html", ignoreCase = true) -> path.removeSuffix("/pair.html")
			else -> path
		}.trimEnd('/')

		return buildString {
			append(uri.scheme)
			append("://")
			append(uri.authority)
			append(cleanedPath)
		}
	}

	fun extractApiKey(url: String): String =
		Uri.parse(url).getQueryParameter("apiKey").orEmpty()

	fun baseUrlFromLiveTvUrl(url: String): String {
		val uri = Uri.parse(url)
		val path = uri.path.orEmpty().replace(Regex("/iptv/(channels\\.m3u|epg\\.xml)$", RegexOption.IGNORE_CASE), "")
		return buildString {
			append(uri.scheme ?: "http")
			append("://")
			append(uri.authority.orEmpty())
			append(path.trimEnd('/'))
		}
	}
}
