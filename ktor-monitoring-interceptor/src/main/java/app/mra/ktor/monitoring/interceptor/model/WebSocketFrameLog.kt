package app.mra.ktor.monitoring.interceptor.model

enum class FrameDirection {
    SENT, RECEIVED
}

enum class FrameType {
    TEXT, BINARY, PING, PONG, CLOSE
}

data class WebSocketFrameLog(
    val id: String,
    val direction: FrameDirection,
    val type: FrameType,
    val textPayload: String? = null,
    val binaryData: ByteArray? = null,
    val byteSize: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    var cachedAudioPath: String? = null,
    val isAudio: Boolean = true,
    val mimeType: String = "audio/wav"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebSocketFrameLog) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
