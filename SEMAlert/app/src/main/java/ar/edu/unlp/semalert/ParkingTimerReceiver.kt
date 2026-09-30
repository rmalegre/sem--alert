package ar.edu.unlp.semalert

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ParkingTimerReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TIMER_WARNING = "ar.edu.unlp.semalert.ACTION_TIMER_WARNING"
        const val ACTION_TIMER_EXPIRED = "ar.edu.unlp.semalert.ACTION_TIMER_EXPIRED"
        private const val REQ_WARNING = 2001
        private const val REQ_EXPIRED = 2002

        fun scheduleTimer(context: Context, endMillis: Long, leadMinutes: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // Cancel any previous alarms
            cancelTimer(context)

            val now = System.currentTimeMillis()
            val warningMillis = endMillis - (leadMinutes * 60 * 1000L)

            // Warning alarm (only if warning time is in the future)
            if (warningMillis > now) {
                val warningIntent = Intent(context, ParkingTimerReceiver::class.java).apply {
                    action = ACTION_TIMER_WARNING
                    putExtra("leadMinutes", leadMinutes)
                }
                val warningPending = PendingIntent.getBroadcast(
                    context,
                    REQ_WARNING,
                    warningIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        warningMillis,
                        warningPending
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        warningMillis,
                        warningPending
                    )
                }
            }

            // Expiration alarm
            if (endMillis > now) {
                val expiredIntent = Intent(context, ParkingTimerReceiver::class.java).apply {
                    action = ACTION_TIMER_EXPIRED
                }
                val expiredPending = PendingIntent.getBroadcast(
                    context,
                    REQ_EXPIRED,
                    expiredIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        endMillis,
                        expiredPending
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        endMillis,
                        expiredPending
                    )
                }
            }
        }

        fun cancelTimer(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val warningIntent = Intent(context, ParkingTimerReceiver::class.java).apply {
                action = ACTION_TIMER_WARNING
            }
            val warningPending = PendingIntent.getBroadcast(
                context,
                REQ_WARNING,
                warningIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (warningPending != null) {
                alarmManager.cancel(warningPending)
                warningPending.cancel()
            }

            val expiredIntent = Intent(context, ParkingTimerReceiver::class.java).apply {
                action = ACTION_TIMER_EXPIRED
            }
            val expiredPending = PendingIntent.getBroadcast(
                context,
                REQ_EXPIRED,
                expiredIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (expiredPending != null) {
                alarmManager.cancel(expiredPending)
                expiredPending.cancel()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val store = ParkingStore(context)
        val parking = store.getParking()

        when (intent.action) {
            ACTION_TIMER_WARNING -> {
                val leadMinutes = intent.getIntExtra("leadMinutes", 10)
                NotificationHelper.showTimerWarning(context, leadMinutes)
                store.setTimerAlertTriggered(true)
            }
            ACTION_TIMER_EXPIRED -> {
                NotificationHelper.showTimerExpired(context)
                if (parking != null) {
                    NotificationHelper.showParkingStatus(
                        context,
                        parking.address,
                        "⚠️ Tiempo vencido"
                    )
                }
            }
        }
    }
}
