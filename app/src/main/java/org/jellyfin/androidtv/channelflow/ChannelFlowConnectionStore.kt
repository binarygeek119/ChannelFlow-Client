package org.jellyfin.androidtv.channelflow

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

	private val _state = MutableStateFlow(load().normalized())
	val state: StateFlow<ChannelFlowServersState> = _state.asStateFlow()

	val connection: ChannelFlowConnection?
		get() = _state.value.connection

	val isConnected: Boolean
		get() = connection != null

	val servers: List<ChannelFlowSavedServer>
		get() = _state.value.servers

	val activeServerId: String?
		get() = _state.value.activeServerId

	init {
		write()
	}

	fun save(connection: ChannelFlowConnection) {
		val current = _state.value
		val existing = current.servers.firstOrNull { it.connection.baseUrl.equals(connection.baseUrl, ignoreCase = true) }
		val server = existing?.copy(connection = connection)
			?: ChannelFlowSavedServer(id = UUID.randomUUID().toString(), connection = connection)
		val servers = current.servers.filterNot { it.id == server.id } + server
		commit(
			current.copy(
				servers = servers,
				activeServerId = server.id,
				connection = server.connection,
			)
		)
	}

	fun setActive(serverId: String): Boolean {
		val server = _state.value.servers.firstOrNull { it.id == serverId } ?: return false
		commit(
			_state.value.copy(
				activeServerId = server.id,
				connection = server.connection,
			)
		)
		return true
	}

	fun remove(serverId: String) {
		val remaining = _state.value.servers.filterNot { it.id == serverId }
		val next = remaining.firstOrNull { it.id == _state.value.activeServerId } ?: remaining.firstOrNull()
		commit(
			_state.value.copy(
				servers = remaining,
				activeServerId = next?.id,
				connection = next?.connection,
			)
		)
	}

	fun clearConnection() {
		commit(
			_state.value.copy(
				servers = emptyList(),
				activeServerId = null,
				connection = null,
			)
		)
	}

	fun isFavorite(channelId: UUID): Boolean =
		_state.value.favoriteChannelIds.contains(channelId.toString())

	fun setFavorite(channelId: UUID, favorite: Boolean) {
		val ids = _state.value.favoriteChannelIds.toMutableSet()
		if (favorite) ids.add(channelId.toString()) else ids.remove(channelId.toString())
		commit(_state.value.copy(favoriteChannelIds = ids))
	}

	fun favoriteIds(): Set<UUID> =
		_state.value.favoriteChannelIds.mapNotNull { it.toUUIDOrNull() }.toSet()

	private fun load(): StoredState {
		if (!file.exists()) return StoredState()
		return runCatching {
			json.decodeFromString<StoredState>(file.readText())
		}.onFailure { error ->
			Timber.e(error, "Unable to read ChannelFlow connection store")
		}.getOrDefault(StoredState())
	}

	private fun commit(next: ChannelFlowServersState) {
		_state.value = next
		write()
	}

	private fun write() {
		val snapshot = _state.value
		runCatching {
			file.writeText(
				json.encodeToString(
					StoredState(
						connection = snapshot.connection,
						servers = snapshot.servers,
						activeServerId = snapshot.activeServerId,
						favoriteChannelIds = snapshot.favoriteChannelIds,
					)
				)
			)
		}.onFailure { error ->
			Timber.e(error, "Unable to write ChannelFlow connection store")
		}
	}

	private fun StoredState.normalized(): ChannelFlowServersState {
		var nextServers = servers
		var nextActiveId = activeServerId
		if (nextServers.isEmpty() && connection != null) {
			nextActiveId = UUID.randomUUID().toString()
			nextServers = listOf(ChannelFlowSavedServer(id = nextActiveId, connection = connection))
		}
		val active = nextServers.firstOrNull { it.id == nextActiveId } ?: nextServers.firstOrNull()
		return ChannelFlowServersState(
			servers = nextServers,
			activeServerId = active?.id,
			connection = active?.connection,
			favoriteChannelIds = favoriteChannelIds,
		)
	}

	@Serializable
	private data class StoredState(
		val connection: ChannelFlowConnection? = null,
		val servers: List<ChannelFlowSavedServer> = emptyList(),
		val activeServerId: String? = null,
		val favoriteChannelIds: Set<String> = emptySet(),
	)
}

data class ChannelFlowServersState(
	val servers: List<ChannelFlowSavedServer> = emptyList(),
	val activeServerId: String? = null,
	val connection: ChannelFlowConnection? = null,
	val favoriteChannelIds: Set<String> = emptySet(),
)
