package ar.edu.unlp.semalert

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AlertType {
    PARKED,
    GEOFENCE_ENTER,
    GEOFENCE_EXIT,
    TIMER_WARNING,
    TIMER_EXPIRED,
    SYSTEM_TEST
}

data class AlertLog(
    val id: String,
    val timestamp: Long,
    val title: String,
    val message: String,
    val type: AlertType
)

class NotificationLogStore(context: Context) {
    private val prefs = context.getSharedPreferences("notification_logs", Context.MODE_PRIVATE)

    fun getLogs(): List<AlertLog> {
        val raw = prefs.getString("logs", "[]") ?: "[]"
        val array = JSONArray(raw)
        val result = mutableListOf<AlertLog>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val typeStr = obj.optString("type", AlertType.PARKED.name)
            val type = try { AlertType.valueOf(typeStr) } catch (e: Exception) { AlertType.PARKED }
            result.add(
                AlertLog(
                    id = obj.getString("id"),
                    timestamp = obj.getLong("timestamp"),
                    title = obj.getString("title"),
                    message = obj.getString("message"),
                    type = type
                )
            )
        }
        return result.sortedByDescending { it.timestamp }
    }

    fun addLog(title: String, message: String, type: AlertType) {
        val current = getLogs().toMutableList()
        val newLog = AlertLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            title = title,
            message = message,
            type = type
        )
        current.add(0, newLog)
        // Keep last 50 logs max
        val trimmed = if (current.size > 50) current.subList(0, 50) else current

        val array = JSONArray()
        trimmed.forEach {
            val o = JSONObject().apply {
                put("id", it.id)
                put("timestamp", it.timestamp)
                put("title", it.title)
                put("message", it.message)
                put("type", it.type.name)
            }
            array.put(o)
        }
        prefs.edit().putString("logs", array.toString()).apply()
    }

    fun clear() {
        prefs.edit().putString("logs", "[]").apply()
    }
}
