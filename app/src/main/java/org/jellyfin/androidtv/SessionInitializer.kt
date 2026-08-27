package org.jellyfin.androidtv

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository
import org.jellyfin.androidtv.channelflow.ChannelFlowUpdateChecker
import org.jellyfin.androidtv.di.KoinInitializer

@Suppress("unused")
class SessionInitializer : Initializer<Unit> {
	override fun create(context: Context) {
		val koin = AppInitializer.getInstance(context)
			.initializeComponent(KoinInitializer::class.java)
			.koin

		val scope = ProcessLifecycleOwner.get().lifecycleScope
		scope.launch(Dispatchers.IO) {
			koin.get<SessionRepository>().restoreSession(destroyOnly = false)
		}
		scope.launch(Dispatchers.IO) {
			koin.get<ChannelFlowGuideRepository>().prefetchLatest()
		}
		scope.launch(Dispatchers.IO) {
			koin.get<ChannelFlowUpdateChecker>().prefetch()
		}
	}

	override fun dependencies() = listOf(KoinInitializer::class.java)
}
