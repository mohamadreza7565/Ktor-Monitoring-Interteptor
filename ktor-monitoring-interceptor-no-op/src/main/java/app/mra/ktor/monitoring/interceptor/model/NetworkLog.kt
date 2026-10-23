package app.mra.ktor.monitoring.interceptor.model

sealed interface NetworkLog {
    val id: String
    val url: String
    val timestamp: Long

    data class HttpLog(
        override val id: String = "",
        override val url: String = "",
        override val timestamp: Long = System.currentTimeMillis(),
        val method: String = "",
        val requestHeaders: Map<String, List<String>> = emptyMap(),
        val requestBody: String? = null,
        val queryParams: Map<String, List<String>> = emptyMap(),
        val statusCode: Int? = null,
        val statusText: String? = null,
        val responseHeaders: Map<String, List<String>> = emptyMap(),
        val responseBody: String? = null,
        val contentType: String? = null,
        val contentLength: Long? = null,
        val durationMs: Long? = null,
        val error: String? = null,
        val cachedAudioPath: String? = null
    ) : NetworkLog

    data class WebSocketLog(
        override val id: String = "",
        override val url: String = "",
        override val timestamp: Long = System.currentTimeMillis(),
        val requestHeaders: Map<String, List<String>> = emptyMap(),
        val statusCode: Int? = 101,
        val status: WsStatus = WsStatus.CLOSED,
        val frames: List<WebSocketFrameLog> = emptyList(),
        val closeReason: String? = null,
        val durationMs: Long? = null
    ) : NetworkLog
}

enum class WsStatus {
    CONNECTING, CONNECTED, CLOSING, CLOSED, ERROR
}
