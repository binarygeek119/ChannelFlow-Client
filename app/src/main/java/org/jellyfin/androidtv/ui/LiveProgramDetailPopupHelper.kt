package org.jellyfin.androidtv.ui

import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.util.getActivity
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.android.ext.android.inject

fun LiveProgramDetailPopup.toggleFavorite(
	item: BaseItemDto,
	callback: (item: BaseItemDto) -> Unit,
) {
	val itemMutationRepository by mContext.getActivity()!!.inject<ItemMutationRepository>()

	lifecycle.coroutineScope.launch {
		runCatching {
			val userData = itemMutationRepository.setFavorite(
				item = item.id,
				favorite = !(item.userData?.isFavorite ?: false)
			)

			item.copy(userData = userData)
		}.onSuccess { item ->
			callback(item)
		}
	}
}
