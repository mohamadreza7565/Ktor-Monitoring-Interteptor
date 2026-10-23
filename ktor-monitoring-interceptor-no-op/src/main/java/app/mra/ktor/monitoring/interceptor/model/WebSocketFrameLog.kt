package app.mra.ktor.monitoring.interceptor.model

data class WebSocketFrameLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val direction: FrameDirection = FrameDirection.SENT,
    val type: FrameType = FrameType.TEXT,
    val textPayload: String? = null,
    val binaryData: ByteArray? = null,
    val byteSize: Int = 0,
    val cachedAudioPath: String? = null,
    val isAudio: Boolean = false,
    val mimeType: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WebSocketFrameLog
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class FrameDirection {
    SENT, RECEIVED
}

enum class FrameType {
    TEXT, BINARY, PING, PONG, CLOSE
}
