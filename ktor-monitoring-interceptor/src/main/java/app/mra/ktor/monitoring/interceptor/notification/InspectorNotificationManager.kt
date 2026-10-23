package app.mra.ktor.monitoring.interceptor.notification

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.mra.ktor.monitoring.interceptor.model.InspectorConfig
import app.mra.ktor.monitoring.interceptor.model.NetworkLog
import app.mra.ktor.monitoring.interceptor.store.InspectorLogStore
import app.mra.ktor.monitoring.interceptor.ui.InspectorActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object InspectorNotificationManager {
    private const val CHANNEL_ID = "ktor_inspector_channel"
    private const val CHANNEL_NAME = "Ktor Inspector Logs"
    private const val NOTIFICATION_ID = 88123

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isInitialized = false

    fun init(context: Context, config: InspectorConfig = InspectorConfig()) {
        if (!config.enabled || !config.showNotification) return

        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active Ktor network & WebSocket logs"
                if (!config.enableNotificationSound) {
                    setSound(null, null)
                }
                enableVibration(config.enableNotificationVibration)
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (!isInitialized) {
            isInitialized = true
            updateNotification(appContext, notificationManager, InspectorLogStore.logs.value)
            InspectorLogStore.logs.onEach { logs ->
                updateNotification(appContext, notificationManager, logs)
            }.launchIn(scope)
        }
    }

    private fun updateNotification(
        context: Context,
        notificationManager: NotificationManager,
        logs: List<NetworkLog>
    ) {
        val httpCount = logs.count { it is NetworkLog.HttpLog }
        val wsCount = logs.count { it is NetworkLog.WebSocketLog }

        val contentText = if (logs.isEmpty()) {
            "No network traffic recorded"
        } else {
            "Recorded: $httpCount HTTP calls | $wsCount WebSockets"
        }

        val intent = Intent(context, InspectorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Ktor Inspector")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        runCatching {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}
