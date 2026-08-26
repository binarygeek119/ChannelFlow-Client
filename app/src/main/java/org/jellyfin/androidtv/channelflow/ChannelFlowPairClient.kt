package org.jellyfin.androidtv.channelflow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChannelFlowPairClient {
	companion object {
		const val PIN_SERVER_URL = "http://channelflow.duckdns.org"
	}

	private val json = Json { ignoreUnknownKeys = true }

	private val http = OkHttpClient.Builder()
		.connectTimeout(15, TimeUnit.SECONDS)
		.readTimeout(11, TimeUnit.MINUTES)
		.writeTimeout(15, TimeUnit.SECONDS)
		.pingInterval(15, TimeUnit.SECONDS)
		.build()

	fun pinServerUrl(): String = PIN_SERVER_URL.trimEnd('/')

	suspend fun waitForConnection(
		onPin: (String) -> Unit,
	): Result<ChannelFlowConnection> = withContext(Dispatchers.IO) {
		runCatching {
			try {
				waitOnWebSocket(onPin)
			} catch (error: ChannelFlowPairException) {
				throw error
			} catch (error: Exception) {
				Timber.w(error, "Pin server websocket failed; trying long-poll")
				waitOnLongPoll(onPin)
			}
		}.onFailure { error ->
			Timber.w(error, "ChannelFlow pin wait failed")
		}
	}

	private suspend fun waitOnWebSocket(onPin: (String) -> Unit): ChannelFlowConnection {
		val url = pinServerUrl().replace(Regex("^http"), "ws") + "/v1/wait"
		val request = Request.Builder().url(url).build()
		return suspendCancellableCoroutine { continuation ->
			val socket = http.newWebSocket(request, object : WebSocketListener() {
				private var pin: String? = null

				override fun onMessage(webSocket: WebSocket, text: String) {
					handleMessage(text, onPin, { pin }) { pin = it }
						?.let { complete(webSocket, it) }
				}

				override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
					if (continuation.isActive) {
						continuation.resumeWithException(t)
					}
				}

				override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
					if (continuation.isActive) {
						continuation.resumeWithException(
							ChannelFlowPairException(ChannelFlowPairException.Kind.FAILED)
						)
					}
				}

				private fun complete(webSocket: WebSocket, result: Result<ChannelFlowConnection>) {
					if (!continuation.isActive) return
					result.fold(
						onSuccess = { continuation.resume(it) },
						onFailure = { continuation.resumeWithException(it) },
					)
					webSocket.close(1000, null)
				}
			})
			continuation.invokeOnCancellation { socket.cancel() }
		}
	}

	private suspend fun waitOnLongPoll(onPin: (String) -> Unit): ChannelFlowConnection {
		val request = Request.Builder()
			.url("${pinServerUrl()}/v1/wait")
			.header("Accept", "application/x-ndjson")
			.get()
			.build()
		http.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				error("Pin server returned HTTP ${response.code}")
			}
			val source = response.body?.source() ?: error("empty pin server body")
			var pin: String? = null
			while (!source.exhausted()) {
				val line = source.readUtf8Line() ?: break
				if (line.isBlank()) continue
				val handled = handleMessage(line, onPin, { pin }) { pin = it }
				if (handled != null) {
					return handled.getOrThrow()
				}
			}
			throw ChannelFlowPairException(ChannelFlowPairException.Kind.EXPIRED)
		}
	}

	private fun handleMessage(
		text: String,
		onPin: (String) -> Unit,
		currentPin: () -> String?,
		setPin: (String) -> Unit,
	): Result<ChannelFlowConnection>? {
		val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
		val type = obj["type"]?.jsonPrimitive?.contentOrNull
		val issued = obj["pin"]?.jsonPrimitive?.contentOrNull
		if (!issued.isNullOrBlank() && currentPin() == null) {
			setPin(ChannelFlowPinCrypto.normalize(issued))
			onPin(issued)
			return null
		}
		return when (type) {
			"payload" -> {
				val pin = currentPin() ?: return Result.failure(
					ChannelFlowPairException(ChannelFlowPairException.Kind.FAILED)
				)
				val ciphertext = obj["ciphertext"]?.jsonPrimitive?.contentOrNull.orEmpty()
				runCatching { connectionFromCiphertext(pin, ciphertext) }
			}
			"expired" -> Result.failure(ChannelFlowPairException(ChannelFlowPairException.Kind.EXPIRED))
			else -> null
		}
	}

	private fun connectionFromCiphertext(pin: String, ciphertext: String): ChannelFlowConnection {
		val payload = ChannelFlowPinCrypto.decrypt(pin, ciphertext)
		val m3u = payload.m3u
		val epg = payload.xmltv
		if (m3u.isBlank() || epg.isBlank()) {
			error("Pin payload did not include M3U and XMLTV URLs")
		}
		return ChannelFlowConnection(
			baseUrl = ChannelFlowUrls.baseUrlFromLiveTvUrl(m3u),
			m3uUrl = m3u,
			epgUrl = epg,
			apiKey = ChannelFlowUrls.extractApiKey(m3u).ifBlank { ChannelFlowUrls.extractApiKey(epg) },
		)
	}
}

class ChannelFlowPairException(
	val kind: Kind,
	val serverMessage: String? = null,
) : Exception(serverMessage ?: kind.name) {
	enum class Kind { INVALID, EXPIRED, FAILED }
}
