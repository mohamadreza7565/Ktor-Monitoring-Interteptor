package app.mra.ktor.monitoring.interceptor.plugin

import app.mra.ktor.monitoring.interceptor.model.InspectorConfig
import app.mra.ktor.monitoring.interceptor.model.NetworkLog
import app.mra.ktor.monitoring.interceptor.model.WsStatus
import app.mra.ktor.monitoring.interceptor.store.InspectorLogStore
import com.google.gson.Gson
import io.ktor.client.call.save
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.client.statement.HttpResponseContainer
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.util.AttributeKey
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import java.util.UUID

val KtorInspectorRequestIdKey = AttributeKey<String>("KtorInspectorRequestId")
val KtorInspectorStartTimeKey = AttributeKey<Long>("KtorInspectorStartTime")

private val gson = Gson()

suspend fun extractRequestBodyText(content: Any?): String? {
    if (content == null) return null
    if (content is OutgoingContent.NoContent) return null
    if (content is String) {
        val trimmed = content.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) return trimmed
        if (trimmed.isNotBlank() && !trimmed.contains("http://") && !trimmed.contains("https://")) return trimmed
        return null
    }
    if (content is ByteArray) {
        return runCatching { String(content, Charsets.UTF_8).ifBlank { null } }.getOrNull()
    }

    return runCatching {
        when (content) {
            is OutgoingContent.ByteArrayContent -> {
                String(content.bytes(), Charsets.UTF_8).ifBlank { null }
            }
            is OutgoingContent.WriteChannelContent -> {
                val channel = ByteChannel(autoFlush = true)
                coroutineScope {
                    launch {
                        runCatching { content.writeTo(channel) }
                        channel.close()
                    }
                    val bytes = channel.readRemaining().readByteArray()
                    String(bytes, Charsets.UTF_8).ifBlank { null }
                }
            }
            is OutgoingContent.ReadChannelContent -> {
                val channel = content.readFrom()
                val bytes = channel.readRemaining().readByteArray()
                String(bytes, Charsets.UTF_8).ifBlank { null }
            }
            else -> {
                val className = content.javaClass.name
                if (className.startsWith("io.ktor") || className.startsWith("java.") || className.startsWith("kotlin.")) {
                    null
                } else {
                    runCatching { gson.toJson(content) }.getOrNull()
                }
            }
        }
    }.getOrNull()
}

val KtorMonitoringInspector = createClientPlugin("KtorMonitoringInspector", ::InspectorConfig) {
    val config = pluginConfig

    if (!config.enabled) return@createClientPlugin

    InspectorLogStore.setMaxRetainedLogs(config.maxRetainedLogs)

    onRequest { request, content ->
        runCatching {
            val requestId = UUID.randomUUID().toString()
            request.attributes.put(KtorInspectorRequestIdKey, requestId)
            request.attributes.put(KtorInspectorStartTimeKey, System.currentTimeMillis())

            val requestHeadersMap = request.headers.build().entries().associate { it.key to it.value }
            val queryParamsMap = request.url.parameters.build().entries().associate { it.key to it.value }

            val requestBodyText = extractRequestBodyText(content)

            val log = NetworkLog.HttpLog(
                id = requestId,
                url = request.url.buildString(),
                method = request.method.value,
                requestHeaders = requestHeadersMap,
                queryParams = queryParamsMap,
                requestBody = requestBodyText
            )

            InspectorLogStore.addOrUpdateLog(log)
        }
    }

    client.sendPipeline.intercept(HttpSendPipeline.Engine) { content ->
        runCatching {
            val request = context
            val requestId = if (request.attributes.contains(KtorInspectorRequestIdKey)) {
                request.attributes[KtorInspectorRequestIdKey]
            } else {
                val newId = UUID.randomUUID().toString()
                request.attributes.put(KtorInspectorRequestIdKey, newId)
                newId
            }

            val requestBodyText = extractRequestBodyText(content)

            if (!requestBodyText.isNullOrBlank()) {
                val existing = InspectorLogStore.getLogById(requestId) as? NetworkLog.HttpLog
                if (existing != null) {
                    InspectorLogStore.addOrUpdateLog(existing.copy(requestBody = requestBodyText))
                } else {
                    val log = NetworkLog.HttpLog(
                        id = requestId,
                        url = request.url.buildString(),
                        method = request.method.value,
                        requestHeaders = request.headers.build().entries().associate { it.key to it.value },
                        queryParams = request.url.parameters.build().entries().associate { it.key to it.value },
                        requestBody = requestBodyText
                    )
                    InspectorLogStore.addOrUpdateLog(log)
                }
            }
        }
    }

    client.responsePipeline.intercept(HttpResponsePipeline.Transform) { (typeInfo, response) ->
        runCatching {
            if (context.response.status == HttpStatusCode.SwitchingProtocols && response is DefaultClientWebSocketSession) {
                val url = context.request.url.toString()
                val wsLogId = createWebSocketInspectorLog(url)
                val delegate = InspectedWebSocketDelegate(response, wsLogId)
                val inspectedSession = DefaultClientWebSocketSession(response.call, delegate)
                proceedWith(HttpResponseContainer(typeInfo, inspectedSession))
                return@intercept
            }
        }
    }

    client.receivePipeline.intercept(HttpReceivePipeline.State) { response ->
        runCatching {
            val httpResponse = response
            val isWebSocket = httpResponse.status == HttpStatusCode.SwitchingProtocols ||
                    httpResponse.headers["Upgrade"]?.equals("websocket", ignoreCase = true) == true ||
                    httpResponse.call.request.url.protocol.name.startsWith("ws")

            val requestId = if (httpResponse.call.request.attributes.contains(KtorInspectorRequestIdKey)) {
                httpResponse.call.request.attributes[KtorInspectorRequestIdKey]
            } else {
                UUID.randomUUID().toString()
            }

            val startTime = if (httpResponse.call.request.attributes.contains(KtorInspectorStartTimeKey)) {
                httpResponse.call.request.attributes[KtorInspectorStartTimeKey]
            } else {
                System.currentTimeMillis()
            }

            val duration = System.currentTimeMillis() - startTime
            val responseHeadersMap = httpResponse.headers.entries().associate { it.key to it.value }

            if (isWebSocket) {
                val wsLog = NetworkLog.WebSocketLog(
                    id = requestId,
                    url = httpResponse.call.request.url.toString(),
                    requestHeaders = httpResponse.call.request.headers.entries().associate { it.key to it.value },
                    statusCode = httpResponse.status.value,
                    status = WsStatus.CONNECTED,
                    durationMs = duration
                )
                InspectorLogStore.addOrUpdateLog(wsLog)
                return@intercept
            }

            val contentType = httpResponse.headers["Content-Type"]
            val contentLength = httpResponse.headers["Content-Length"]?.toLongOrNull()

            val savedCall = httpResponse.call.save()
            val responseBodyText = runCatching { savedCall.response.bodyAsText() }.getOrNull()

            val existingLog = InspectorLogStore.getLogById(requestId) as? NetworkLog.HttpLog
            val requestBody = existingLog?.requestBody

            if (existingLog != null) {
                val updatedLog = existingLog.copy(
                    statusCode = httpResponse.status.value,
                    statusText = httpResponse.status.description,
                    responseHeaders = responseHeadersMap,
                    responseBody = responseBodyText,
                    contentType = contentType,
                    contentLength = contentLength,
                    durationMs = duration
                )
                InspectorLogStore.addOrUpdateLog(updatedLog)
            } else {
                val log = NetworkLog.HttpLog(
                    id = requestId,
                    url = httpResponse.call.request.url.toString(),
                    method = httpResponse.call.request.method.value,
                    requestHeaders = httpResponse.call.request.headers.entries().associate { it.key to it.value },
                    queryParams = httpResponse.call.request.url.parameters.entries().associate { it.key to it.value },
                    requestBody = requestBody,
                    statusCode = httpResponse.status.value,
                    statusText = httpResponse.status.description,
                    responseHeaders = responseHeadersMap,
                    responseBody = responseBodyText,
                    contentType = contentType,
                    contentLength = contentLength,
                    durationMs = duration
                )
                InspectorLogStore.addOrUpdateLog(log)
            }
        }
    }
}

@Deprecated(
    message = "Use KtorMonitoringInspector instead",
    replaceWith = ReplaceWith("KtorMonitoringInspector")
)
val KtorInspector = KtorMonitoringInspector

