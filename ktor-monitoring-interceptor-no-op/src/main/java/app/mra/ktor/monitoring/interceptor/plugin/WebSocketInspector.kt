package app.mra.ktor.monitoring.interceptor.plugin

import app.mra.ktor.monitoring.interceptor.model.WsStatus

fun createWebSocketInspectorLog(url: String, headers: Map<String, List<String>> = emptyMap()): String = ""

fun updateWebSocketStatus(id: String, status: WsStatus, closeReason: String? = null) {}
