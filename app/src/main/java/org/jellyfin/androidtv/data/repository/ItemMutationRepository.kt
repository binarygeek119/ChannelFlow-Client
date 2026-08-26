package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.channelflow.ChannelFlowConnectionStore
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.time.Instant

interface ItemMutationRepository {
	suspend fun setFavorite(item: UUID, favorite: Boolean): UserItemDataDto
	suspend fun setPlayed(item: UUID, played: Boolean): UserItemDataDto
}

class ItemMutationRepositoryImpl(
	private val api: ApiClient,
	private val dataRefreshService: DataRefreshService,
	private val connectionStore: ChannelFlowConnectionStore,
) : ItemMutationRepository {
	override suspend fun setFavorite(item: UUID, favorite: Boolean): UserItemDataDto {
		connectionStore.setFavorite(item, favorite)
		dataRefreshService.lastFavoriteUpdate = Instant.now()
		return UserItemDataDto(
			playbackPositionTicks = 0,
			playCount = 0,
			isFavorite = favorite,
			played = false,
			key = item.toString(),
			itemId = item,
		)
	}

	override suspend fun setPlayed(item: UUID, played: Boolean): UserItemDataDto {
		val response by when {
			played -> withContext(Dispatchers.IO) { api.playStateApi.markPlayedItem(itemId = item) }
			else -> withContext(Dispatchers.IO) { api.playStateApi.markUnplayedItem(itemId = item) }
		}

		return response
	}
}
