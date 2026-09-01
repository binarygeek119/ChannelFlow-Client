package org.jellyfin.androidtv.di

import android.content.Context
import androidx.startup.Initializer
import org.jellyfin.androidtv.LogInitializer
import org.jellyfin.androidtv.channelflow.ChannelFlowClientSession
import org.jellyfin.androidtv.channelflow.ChannelFlowLogShipper
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

class KoinInitializer : Initializer<KoinApplication> {
	override fun create(context: Context): KoinApplication {
		val koin = startKoin {
			androidContext(context)

			modules(
				androidModule,
				appModule,
				authModule,
				playbackModule,
				preferenceModule,
				utilsModule,
			)
		}
		koin.koin.get<ChannelFlowLogShipper>().start()
		koin.koin.get<ChannelFlowClientSession>().start()
		return koin
	}

	override fun dependencies() = listOf(LogInitializer::class.java)
}
