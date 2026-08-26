package org.jellyfin.androidtv.channelflow

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.UUID

class ChannelFlowConnectionStore(
	context: Context,
) {
	private val file = context.filesDir.resolve("channelflow_connection.json")
	private val json = Json {
		ignoreUnknownKeys = true
		encodeDefaults = true
	}

	private var snapshot: StoredState = load()

	val connection: ChannelFlowConnection?
		get() = snapshot.connection

	val isConnected: Boolean
		get() = connection != null

	fun save(connection: ChannelFlowConnection) {
		snapshot = snapshot.copy(connection = connection)
		write()
	}

	fun clearConnection() {
		snapshot = snapshot.copy(connection = null)
		write()
	}

	fun isFavorite(channelId: UUID): Boolean =
		snapshot.favoriteChannelIds.contains(channelId.toString())

	fun setFavorite(channelId: UUID, favorite: Boolean) {
		val ids = snapshot.favoriteChannelIds.toMutableSet()
		if (favorite) ids.add(channelId.toString()) else ids.remove(channelId.toString())
		snapshot = snapshot.copy(favoriteChannelIds = ids)
		write()
	}

	fun favoriteIds(): Set<UUID> =
		snapshot.favoriteChannelIds.mapNotNull { it.toUUIDOrNull() }.toSet()

	private fun load(): StoredState {
		if (!file.exists()) return StoredState()
		return runCatching {
			json.decodeFromString<StoredState>(file.readText())
		}.onFailure { error ->
			Timber.e(error, "Unable to read ChannelFlow connection store")
		}.getOrDefault(StoredState())
	}

	private fun write() {
		runCatching {
			file.writeText(json.encodeToString(snapshot))
		}.onFailure { error ->
			Timber.e(error, "Unable to write ChannelFlow connection store")
		}
	}

	@Serializable
	private data class StoredState(
		val connection: ChannelFlowConnection? = null,
		val favoriteChannelIds: Set<String> = emptySet(),
	)
}
