package app.mra.ktor.monitoring.interceptor.store

import app.mra.ktor.monitoring.interceptor.model.NetworkLog
import app.mra.ktor.monitoring.interceptor.model.WebSocketFrameLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

object InspectorLogStore {
    private var maxLogs = 500
    private val _logs = MutableStateFlow<List<NetworkLog>>(emptyList())
    val logs: StateFlow<List<NetworkLog>> = _logs.asStateFlow()

    private val logsMap = ConcurrentHashMap<String, NetworkLog>()
    private val logsList = mutableListOf<NetworkLog>()

    fun setMaxRetainedLogs(max: Int) {
        maxLogs = max
    }

    @Synchronized
    fun addOrUpdateLog(log: NetworkLog) {
        if (logsMap.containsKey(log.id)) {
            val index = logsList.indexOfFirst { it.id == log.id }
            if (index != -1) {
                logsList[index] = log
            }
        } else {
            if (logsList.size >= maxLogs) {
                val removed = logsList.removeAt(0)
                logsMap.remove(removed.id)
            }
            logsList.add(log)
        }
        logsMap[log.id] = log
        _logs.value = logsList.toList().sortedByDescending { it.timestamp }
    }

    fun getLogById(id: String): NetworkLog? {
        return logsMap[id]
    }

    @Synchronized
    fun addWebSocketFrame(wsLogId: String, frame: WebSocketFrameLog) {
        val existing = logsMap[wsLogId] as? NetworkLog.WebSocketLog ?: return
        val updatedFrames = existing.frames + frame
        val updatedWsLog = existing.copy(frames = updatedFrames)
        addOrUpdateLog(updatedWsLog)
    }

    @Synchronized
    fun clearLogs() {
        logsMap.clear()
        logsList.clear()
        _logs.value = emptyList()
    }
}
