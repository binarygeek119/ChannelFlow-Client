package org.jellyfin.androidtv.channelflow

import android.content.Context
import android.os.Handler
import android.os.Looper
import timber.log.Timber

class ChannelFlowAccessGuard(
	context: Context,
	private val store: ChannelFlowConnectionStore,
	private val catalog: ChannelFlowGuideRepository,
) {
	private val app = context.applicationContext
	private val main = Handler(Looper.getMainLooper())

	fun forgetUnauthorized(connection: ChannelFlowConnection): Boolean {
		val apiKey = connection.apiKey
		if (apiKey.isBlank()) return false
		val wasActive = store.connection?.apiKey == apiKey
		if (!store.removeByApiKey(apiKey)) return false
		Timber.w("Dropped ChannelFlow server %s because its API key was rejected", connection.displayName())
		main.post {
			catalog.clear()
			when {
				!store.isConnected -> app.startChannelFlowPairingFromEmpty()
				wasActive -> app.reloadChannelFlowMain()
			}
		}
		return true
	}
}
