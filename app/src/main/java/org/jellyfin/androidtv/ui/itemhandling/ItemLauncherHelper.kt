package org.jellyfin.androidtv.ui.itemhandling

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.java.KoinJavaComponent
import java.util.UUID

object ItemLauncherHelper {
	@JvmStatic
	fun getItem(itemId: UUID, callback: Response<BaseItemDto>) {
		ProcessLifecycleOwner.get().lifecycleScope.launch {
			val catalog by KoinJavaComponent.inject<ChannelFlowGuideRepository>(ChannelFlowGuideRepository::class.java)

			val item = catalog.getChannel(itemId) ?: catalog.getProgram(itemId)
			if (item != null) callback.onResponse(item)
			else callback.onError(IllegalArgumentException("Unknown item $itemId"))
		}
	}
}
