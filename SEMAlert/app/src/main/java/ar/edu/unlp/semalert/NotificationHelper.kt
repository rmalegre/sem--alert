package ar.edu.unlp.semalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ALERTS_ID = "sem_alerts_channel"
    const val CHANNEL_TIMER_ID = "sem_timer_channel"
    const val CHANNEL_STATUS_ID = "sem_status_channel"

    private const val NOTIF_ID_STATUS = 100
    private const val NOTIF_ID_TIMER = 101

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // 1. Channel Alerts
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Alertas de Geocercas SEM",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos al alejarse del auto o ingresar en zonas de control SEM"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(alarmSound, audioAttributes)
            }

            // 2. Channel Timer
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ID,
                "Temporizador de Estacionamiento",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos previos al vencimiento del ticket de estacionamiento"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(alarmSound, audioAttributes)
            }

            // 3. Channel Status
            val statusChannel = NotificationChannel(
                CHANNEL_STATUS_ID,
                "Estado del Estacionamiento",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el estado activo del auto estacionado"
                setShowBadge(false)
            }

            manager.createNotificationChannel(alertChannel)
            manager.createNotificationChannel(timerChannel)
            manager.createNotificationChannel(statusChannel)
        }
    }

    private fun getOpenAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showAlert(context: Context, title: String, text: String, type: AlertType = AlertType.GEOFENCE_ENTER) {
        createChannels(context)
        NotificationLogStore(context).addLog(title, text, type)

        val settings = AppSettingsStore(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_nav_notifications)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(getOpenAppIntent(context))
            .setAutoCancel(true)

        if (!settings.vibrationEnabled) {
            builder.setVibrate(longArrayOf(0))
        }
        if (!settings.soundEnabled) {
            builder.setSound(null)
        }

        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
    }

    fun showTimerWarning(context: Context, minutesLeft: Int) {
        createChannels(context)
        val title = "⏱️ Estacionamiento por vencer"
        val text = "Te quedan $minutesLeft minutos antes de que venza el tiempo de estacionamiento."
        NotificationLogStore(context).addLog(title, text, AlertType.TIMER_WARNING)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_nav_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getOpenAppIntent(context))
            .setAutoCancel(true)

        manager.notify(NOTIF_ID_TIMER, builder.build())
    }

    fun showTimerExpired(context: Context) {
        createChannels(context)
        val title = "⚠️ ¡Tiempo de estacionamiento vencido!"
        val text = "El tiempo configurado ha llegado a su fin. Renovalo para evitar infracciones."
        NotificationLogStore(context).addLog(title, text, AlertType.TIMER_EXPIRED)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_nav_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(getOpenAppIntent(context))
            .setAutoCancel(true)

        manager.notify(NOTIF_ID_TIMER, builder.build())
    }

    fun showTestNotification(context: Context) {
        createChannels(context)
        showAlert(
            context,
            "🔔 Notificación de prueba SEMAlert",
            "Las notificaciones y el sistema de avisos funcionan correctamente en tu dispositivo.",
            AlertType.SYSTEM_TEST
        )
    }

    fun showParkingStatus(context: Context, address: String, timerText: String?) {
        createChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val text = if (timerText != null) "$address · $timerText" else address

        val builder = NotificationCompat.Builder(context, CHANNEL_STATUS_ID)
            .setSmallIcon(R.drawable.ic_car)
            .setContentTitle("🚗 Auto Estacionado - SEMAlert")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(getOpenAppIntent(context))

        manager.notify(NOTIF_ID_STATUS, builder.build())
    }

    fun cancelParkingStatus(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.cancel(NOTIF_ID_STATUS)
        manager.cancel(NOTIF_ID_TIMER)
    }
}
