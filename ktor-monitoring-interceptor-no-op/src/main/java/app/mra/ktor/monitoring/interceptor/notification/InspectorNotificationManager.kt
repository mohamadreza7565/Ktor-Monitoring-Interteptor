package app.mra.ktor.monitoring.interceptor.notification

import android.content.Context
import app.mra.ktor.monitoring.interceptor.model.NetworkLog

object InspectorNotificationManager {
    fun init(context: Context) {}
    fun showOrUpdateNotification(log: NetworkLog) {}
    fun dismissNotification() {}
}
