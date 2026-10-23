package app.mra.ktor.monitoring.interceptor.store

import app.mra.ktor.monitoring.interceptor.model.NetworkLog
import app.mra.ktor.monitoring.interceptor.model.WebSocketFrameLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object InspectorLogStore {
    private val _logs = MutableStateFlow<List<NetworkLog>>(emptyList())
    val logs: StateFlow<List<NetworkLog>> = _logs.asStateFlow()

    fun setMaxRetainedLogs(max: Int) {}
    fun addOrUpdateLog(log: NetworkLog) {}
    fun addWebSocketFrame(wsLogId: String, frame: WebSocketFrameLog) {}
    fun getLogById(id: String): NetworkLog? = null
    fun clearLogs() {}
}
