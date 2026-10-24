package app.mra.ktor.monitoring.interceptor.model

class InspectorConfig {
    var enabled: Boolean = true
    var maxRetainedLogs: Int = 500
    var showNotification: Boolean = true
    var autoPlayAudio: Boolean = false
    var enableNotificationVibration: Boolean = false
    var enableNotificationSound: Boolean = false
}
