package app.mra.ktor.monitoring.interceptor.plugin

import android.content.Context
import app.mra.ktor.monitoring.interceptor.model.FrameDirection
import app.mra.ktor.monitoring.interceptor.model.FrameType
import app.mra.ktor.monitoring.interceptor.model.NetworkLog
import app.mra.ktor.monitoring.interceptor.model.WebSocketFrameLog
import app.mra.ktor.monitoring.interceptor.model.WsStatus
import app.mra.ktor.monitoring.interceptor.store.InspectorLogStore
import app.mra.ktor.monitoring.interceptor.util.AudioCacheManager
import com.google.gson.Gson
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import java.util.UUID

private val gson = Gson()
private var lastActiveWsLogId: String? = null

@OptIn(ExperimentalCoroutinesApi::class)
class InspectedWebSocketDelegate(
    private val delegate: DefaultWebSocketSession,
    val wsLogId: String,
    private val context: Context? = null
) : DefaultWebSocketSession by delegate {

    override val outgoing: SendChannel<Frame> = object : SendChannel<Frame> by delegate.outgoing {
        override suspend fun send(element: Frame) {
            runCatching {
                val frameLog = processFrame(element, FrameDirection.SENT)
                InspectorLogStore.addWebSocketFrame(wsLogId, frameLog)
            }
            delegate.outgoing.send(element)
        }
    }

    override val incoming: ReceiveChannel<Frame> = CoroutineScope(delegate.coroutineContext).produce(Dispatchers.Unconfined) {
        for (frame in delegate.incoming) {
            runCatching {
                val frameLog = processFrame(frame, FrameDirection.RECEIVED)
                InspectorLogStore.addWebSocketFrame(wsLogId, frameLog)
            }
            send(frame)
        }
    }

    private fun processFrame(frame: Frame, direction: FrameDirection): WebSocketFrameLog {
        val id = UUID.randomUUID().toString()
        return when (frame) {
            is Frame.Text -> {
                WebSocketFrameLog(
                    id = id,
                    direction = direction,
                    type = FrameType.TEXT,
                    textPayload = runCatching { frame.readText() }.getOrNull(),
                    byteSize = frame.data.size
                )
            }
            is Frame.Binary -> {
                val bytes = runCatching { frame.readBytes() }.getOrDefault(frame.data)
                val fileInfo = AudioCacheManager.detectFileType(bytes)
                WebSocketFrameLog(
                    id = id,
                    direction = direction,
                    type = FrameType.BINARY,
                    binaryData = bytes,
                    byteSize = bytes.size,
                    cachedAudioPath = null,
                    isAudio = fileInfo.isAudio,
                    mimeType = fileInfo.mimeType
                )
            }
            is Frame.Ping -> {
                WebSocketFrameLog(id = id, direction = direction, type = FrameType.PING, byteSize = frame.data.size)
            }
            is Frame.Pong -> {
                WebSocketFrameLog(id = id, direction = direction, type = FrameType.PONG, byteSize = frame.data.size)
            }
            is Frame.Close -> {
                WebSocketFrameLog(id = id, direction = direction, type = FrameType.CLOSE, byteSize = frame.data.size)
            }
            else -> {
                WebSocketFrameLog(id = id, direction = direction, type = FrameType.BINARY, byteSize = frame.data.size)
            }
        }
    }
}

fun createWebSocketInspectorLog(url: String, headers: Map<String, List<String>> = emptyMap()): String {
    val id = UUID.randomUUID().toString()
    lastActiveWsLogId = id
    runCatching {
        val wsLog = NetworkLog.WebSocketLog(
            id = id,
            url = url,
            requestHeaders = headers,
            status = WsStatus.CONNECTED
        )
        InspectorLogStore.addOrUpdateLog(wsLog)
    }
    return id
}

fun updateWebSocketStatus(id: String, status: WsStatus, closeReason: String? = null) {
    runCatching {
        val existing = InspectorLogStore.getLogById(id) as? NetworkLog.WebSocketLog ?: return
        val updated = existing.copy(status = status, closeReason = closeReason)
        InspectorLogStore.addOrUpdateLog(updated)
    }
}
