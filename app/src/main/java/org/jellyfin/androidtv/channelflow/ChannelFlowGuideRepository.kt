package org.jellyfin.androidtv.channelflow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jellyfin.androidtv.preference.LiveTvPreferences
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ChannelType
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.LocationType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.UserItemDataDto
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

class ChannelFlowGuideRepository(
	private val store: ChannelFlowConnectionStore,
	private val okHttpFactory: OkHttpFactory,
	private val httpClientOptions: HttpClientOptions,
	private val liveTvPreferences: LiveTvPreferences,
) {
	private val mutex = Mutex()
	private var channels: List<ChannelFlowChannel> = emptyList()
	private var programs: List<ChannelFlowProgram> = emptyList()
	private var loadedAt: Long = 0L

	suspend fun refresh(force: Boolean = false) {
		val connection = store.connection ?: run {
			mutex.withLock {
				channels = emptyList()
				programs = emptyList()
				loadedAt = 0L
			}
			return
		}

		mutex.withLock {
			val stale = System.currentTimeMillis() - loadedAt > CACHE_TTL_MS
			if (!force && !stale && channels.isNotEmpty()) return
		}

		val nextChannels = runCatching { fetchText(connection.m3uUrl).let(M3uParser::parse) }
			.onFailure { Timber.w(it, "Unable to load ChannelFlow M3U") }
			.getOrDefault(emptyList())
		val nextPrograms = runCatching { fetchText(connection.epgUrl).let(XmltvParser::parse) }
			.onFailure { Timber.w(it, "Unable to load ChannelFlow XMLTV") }
			.getOrDefault(emptyList())

		mutex.withLock {
			channels = nextChannels
			programs = nextPrograms
			loadedAt = System.currentTimeMillis()
		}
	}

	fun getStreamUrl(itemId: UUID): String? {
		channels.firstOrNull { it.id == itemId }?.streamUrl?.let { return it }
		val program = programs.firstOrNull { it.id == itemId } ?: return null
		return channels.firstOrNull { it.id == program.channelId }?.streamUrl
	}

	fun getLogoUrl(itemId: UUID): String? {
		channels.firstOrNull { it.id == itemId }?.logoUrl?.let { return it }
		return programs.firstOrNull { it.id == itemId }?.iconUrl
	}

	suspend fun getChannels(): List<BaseItemDto> {
		refresh()
		val now = LocalDateTime.now()
		val favsAtTop = liveTvPreferences[LiveTvPreferences.favsAtTop]
		return mutex.withLock {
			channels
				.map { it.toBaseItem(now) }
				.sortedWith(
					compareByDescending<BaseItemDto> { favsAtTop && it.userData?.isFavorite == true }
						.thenBy { channelSortKey(it.number) }
						.thenBy { it.name.orEmpty() }
				)
		}
	}

	suspend fun getChannel(id: UUID): BaseItemDto? {
		refresh()
		return mutex.withLock {
			channels.firstOrNull { it.id == id }?.toBaseItem(LocalDateTime.now())
		}
	}

	suspend fun getPrograms(
		channelIds: Collection<UUID>,
		startTime: LocalDateTime,
		endTime: LocalDateTime,
	): List<BaseItemDto> {
		refresh()
		val ids = channelIds.toSet()
		return mutex.withLock {
			programs
				.filter { it.channelId in ids && it.start < endTime && it.end > startTime }
				.sortedBy { it.start }
				.map { it.toBaseItem() }
		}
	}

	suspend fun getProgram(id: UUID): BaseItemDto? {
		refresh()
		return mutex.withLock {
			programs.firstOrNull { it.id == id }?.toBaseItem()
		}
	}

	fun clear() {
		channels = emptyList()
		programs = emptyList()
		loadedAt = 0L
	}

	private fun ChannelFlowChannel.toBaseItem(now: LocalDateTime): BaseItemDto {
		val current = programs.firstOrNull { it.channelId == id && it.start <= now && it.end > now }
		return BaseItemDto(
			id = id,
			name = name,
			number = number,
			type = BaseItemKind.TV_CHANNEL,
			mediaType = MediaType.VIDEO,
			channelType = ChannelType.TV,
			locationType = LocationType.VIRTUAL,
			imageTags = logoUrl?.let { mapOf(ImageType.PRIMARY to it, ImageType.LOGO to it) },
			primaryImageAspectRatio = 1.0,
			currentProgram = current?.toBaseItem(),
			userData = userData(id),
		)
	}

	private fun ChannelFlowProgram.toBaseItem(): BaseItemDto = BaseItemDto(
		id = id,
		name = title,
		episodeTitle = episodeTitle,
		overview = overview,
		type = BaseItemKind.PROGRAM,
		mediaType = MediaType.VIDEO,
		channelId = channelId,
		parentId = channelId,
		startDate = start,
		endDate = end,
		premiereDate = start,
		officialRating = officialRating,
		productionYear = productionYear,
		genres = categories.takeIf { it.isNotEmpty() },
		imageTags = iconUrl?.let { mapOf(ImageType.PRIMARY to it, ImageType.THUMB to it) },
		runTimeTicks = java.time.Duration.between(start, end).seconds.coerceAtLeast(0) * 10_000_000,
	)

	private fun userData(channelId: UUID) = UserItemDataDto(
		playbackPositionTicks = 0,
		playCount = 0,
		isFavorite = store.isFavorite(channelId),
		played = false,
		key = channelId.toString(),
		itemId = channelId,
	)

	private suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
		val request = Request.Builder()
			.url(url)
			.header("Accept", "*/*")
			.get()
			.build()
		okHttpFactory.createClient(httpClientOptions).newCall(request).execute().use { response ->
			if (!response.isSuccessful) error("HTTP ${response.code} for $url")
			response.body?.string().orEmpty()
		}
	}

	private fun channelSortKey(number: String?): String {
		if (number.isNullOrBlank()) return "~"
		val parts = number.split('.', '-', ' ')
		val major = parts.getOrNull(0)?.padStart(6, '0') ?: "000000"
		val minor = parts.getOrNull(1)?.padStart(4, '0') ?: "0000"
		return "$major.$minor"
	}

	companion object {
		private const val CACHE_TTL_MS = 2 * 60 * 1000L
	}
}
