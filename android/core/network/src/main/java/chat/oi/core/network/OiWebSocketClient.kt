package chat.oi.core.network

import chat.oi.core.network.model.ClientWsFrame
import chat.oi.core.network.model.ServerWsFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    SESSION_TERMINATED
}

class OiWebSocketClient(
    private val serverUrl: String,
    private val sessionTokenProvider: suspend () -> String?
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _incomingEvents = MutableSharedFlow<ServerWsFrame>()
    val incomingEvents: SharedFlow<ServerWsFrame> = _incomingEvents.asSharedFlow()

    private var retryCount = 0

    fun connect() {
        if (_connectionStatus.value == ConnectionStatus.CONNECTING || _connectionStatus.value == ConnectionStatus.CONNECTED) {
            return
        }

        _connectionStatus.value = ConnectionStatus.CONNECTING
        val request = Request.Builder().url(serverUrl).build()

        activeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                retryCount = 0
                scope.launch {
                    val token = sessionTokenProvider()
                    if (token != null) {
                        sendFrame(ClientWsFrame.Auth(token))
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val frame = json.decodeFromString<ServerWsFrame>(text)
                    if (frame is ServerWsFrame.SessionTerminated) {
                        _connectionStatus.value = ConnectionStatus.SESSION_TERMINATED
                    }
                    scope.launch { _incomingEvents.emit(frame) }
                } catch (e: Exception) {
                    // Ignore or log malformed packet in debug mode
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    fun sendFrame(frame: ClientWsFrame): Boolean {
        val payload = json.encodeToString(frame)
        return activeWebSocket?.send(payload) ?: false
    }

    private fun scheduleReconnect() {
        if (_connectionStatus.value == ConnectionStatus.SESSION_TERMINATED) return

        scope.launch {
            // Exponential backoff: 1s, 2s, 4s, 8s, 16s... capped at 60s with ±20% jitter
            val baseDelay = minOf(60_000L, (1000L * (1 shl minOf(retryCount, 6))))
            val jitter = (baseDelay * (Random.nextDouble(-0.2, 0.2))).toLong()
            val finalDelay = maxOf(500L, baseDelay + jitter)

            retryCount++
            delay(finalDelay)
            connect()
        }
    }

    fun disconnect() {
        activeWebSocket?.close(1000, "Normal closure")
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }
}
