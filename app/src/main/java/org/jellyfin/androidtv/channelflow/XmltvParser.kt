package org.jellyfin.androidtv.channelflow

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ChannelFlowProgram(
	val id: UUID,
	val channelId: UUID,
	val title: String,
	val episodeTitle: String?,
	val overview: String?,
	val start: LocalDateTime,
	val end: LocalDateTime,
	val iconUrl: String?,
	val categories: List<String>,
	val officialRating: String?,
	val productionYear: Int?,
)

object XmltvParser {
	private val withOffset = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z")
	private val withoutOffset = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

	fun parse(xml: String): List<ChannelFlowProgram> {
		val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
			setInput(StringReader(xml))
		}

		val programs = mutableListOf<ChannelFlowProgram>()
		var event = parser.eventType
		while (event != XmlPullParser.END_DOCUMENT) {
			if (event == XmlPullParser.START_TAG && parser.name.equals("programme", ignoreCase = true)) {
				parseProgramme(parser)?.let(programs::add)
			}
			event = parser.next()
		}
		return programs
	}

	private fun parseProgramme(parser: XmlPullParser): ChannelFlowProgram? {
		val channelId = ChannelFlowIds.parse(parser.getAttributeValue(null, "channel")) ?: return null
		val start = parseTime(parser.getAttributeValue(null, "start")) ?: return null
		val end = parseTime(parser.getAttributeValue(null, "stop")) ?: return null

		var title = ""
		var episodeTitle: String? = null
		var overview: String? = null
		var iconUrl: String? = null
		var officialRating: String? = null
		var productionYear: Int? = null
		val categories = mutableListOf<String>()

		var event = parser.next()
		while (!(event == XmlPullParser.END_TAG && parser.name.equals("programme", ignoreCase = true))) {
			if (event == XmlPullParser.START_TAG) {
				when (parser.name.lowercase()) {
					"title" -> title = readText(parser)
					"sub-title" -> episodeTitle = readText(parser).ifBlank { null }
					"desc" -> overview = readText(parser).ifBlank { null }
					"category" -> readText(parser).ifBlank { null }?.let(categories::add)
					"icon" -> iconUrl = parser.getAttributeValue(null, "src")?.ifBlank { null }
					"date" -> productionYear = readText(parser).take(4).toIntOrNull()
					"rating" -> {
						// nested <value>
					}
					"value" -> if (officialRating == null) officialRating = readText(parser).ifBlank { null }
				}
			}
			event = parser.next()
			if (event == XmlPullParser.END_DOCUMENT) break
		}

		if (title.isBlank()) return null
		val id = UUID.nameUUIDFromBytes("$channelId|$start|$title".toByteArray())
		return ChannelFlowProgram(
			id = id,
			channelId = channelId,
			title = title,
			episodeTitle = episodeTitle,
			overview = overview,
			start = start,
			end = end,
			iconUrl = iconUrl,
			categories = categories,
			officialRating = officialRating,
			productionYear = productionYear,
		)
	}

	private fun readText(parser: XmlPullParser): String {
		if (parser.next() != XmlPullParser.TEXT) return ""
		val text = parser.text.orEmpty().trim()
		parser.nextTag()
		return text
	}

	internal fun parseTime(raw: String?): LocalDateTime? {
		if (raw.isNullOrBlank()) return null
		val value = raw.trim()
		return runCatching {
			OffsetDateTime.parse(value, withOffset)
				.atZoneSameInstant(ZoneId.systemDefault())
				.toLocalDateTime()
		}.recoverCatching {
			val local = LocalDateTime.parse(value.take(14), withoutOffset)
			local.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
		}.getOrNull()
	}
}
