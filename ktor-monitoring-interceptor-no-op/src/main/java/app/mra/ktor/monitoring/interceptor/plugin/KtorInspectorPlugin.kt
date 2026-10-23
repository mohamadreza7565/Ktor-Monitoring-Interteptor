package app.mra.ktor.monitoring.interceptor.plugin

import app.mra.ktor.monitoring.interceptor.model.InspectorConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.util.AttributeKey

val KtorInspectorRequestIdKey = AttributeKey<String>("KtorInspectorRequestId")
val KtorInspectorStartTimeKey = AttributeKey<Long>("KtorInspectorStartTime")

suspend fun extractRequestBodyText(content: Any?): String? = null

val KtorInspector = createClientPlugin("KtorInspector", ::InspectorConfig) {
    // No-Op: Does nothing
}
