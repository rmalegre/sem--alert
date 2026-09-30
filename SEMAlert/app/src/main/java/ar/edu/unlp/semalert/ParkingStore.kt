package ar.edu.unlp.semalert

import android.content.Context
import org.json.JSONObject

data class ParkingSession(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val parkedTimeMillis: Long,
    val notes: String = "",
    val alertRadiusMeters: Float = 100f,
    val timerDurationMinutes: Int = 0,
    val timerEndMillis: Long = 0L,
    val timerAlertLeadMinutes: Int = 10,
    val timerAlertTriggered: Boolean = false
) {
    val isTimerActive: Boolean
        get() = timerEndMillis > System.currentTimeMillis()

    val remainingMinutes: Int
        get() {
            val diff = timerEndMillis - System.currentTimeMillis()
            return if (diff > 0) (diff / 60000L).toInt() else 0
        }
}

class ParkingStore(context: Context) {
    private val prefs = context.getSharedPreferences("parking_session", Context.MODE_PRIVATE)

    fun getParking(): ParkingSession? {
        val raw = prefs.getString("current_session", null) ?: return null
        return try {
            val json = JSONObject(raw)
            ParkingSession(
                latitude = json.getDouble("lat"),
                longitude = json.getDouble("lon"),
                address = json.optString("address", "Estacionamiento"),
                parkedTimeMillis = json.optLong("parkedTime", System.currentTimeMillis()),
                notes = json.optString("notes", ""),
                alertRadiusMeters = json.optDouble("radius", 100.0).toFloat(),
                timerDurationMinutes = json.optInt("timerDuration", 0),
                timerEndMillis = json.optLong("timerEnd", 0L),
                timerAlertLeadMinutes = json.optInt("timerAlertLead", 10),
                timerAlertTriggered = json.optBoolean("timerAlertTriggered", false)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveParking(
        latitude: Double,
        longitude: Double,
        address: String,
        notes: String = "",
        radius: Float = 100f,
        timerDurationMinutes: Int = 0,
        timerEndMillis: Long = 0L,
        timerAlertLeadMinutes: Int = 10
    ): ParkingSession {
        val session = ParkingSession(
            latitude = latitude,
            longitude = longitude,
            address = address,
            parkedTimeMillis = System.currentTimeMillis(),
            notes = notes,
            alertRadiusMeters = radius,
            timerDurationMinutes = timerDurationMinutes,
            timerEndMillis = timerEndMillis,
            timerAlertLeadMinutes = timerAlertLeadMinutes,
            timerAlertTriggered = false
        )
        persist(session)
        return session
    }

    fun updateRadius(radius: Float) {
        val current = getParking() ?: return
        persist(current.copy(alertRadiusMeters = radius))
    }

    fun updateTimer(endMillis: Long, durationMinutes: Int, leadMinutes: Int = 10) {
        val current = getParking() ?: return
        persist(current.copy(
            timerEndMillis = endMillis,
            timerDurationMinutes = durationMinutes,
            timerAlertLeadMinutes = leadMinutes,
            timerAlertTriggered = false
        ))
    }

    fun setTimerAlertTriggered(triggered: Boolean) {
        val current = getParking() ?: return
        persist(current.copy(timerAlertTriggered = triggered))
    }

    fun updateNotes(notes: String) {
        val current = getParking() ?: return
        persist(current.copy(notes = notes))
    }

    fun clearParking() {
        prefs.edit().remove("current_session").apply()
    }

    private fun persist(session: ParkingSession) {
        val json = JSONObject().apply {
            put("lat", session.latitude)
            put("lon", session.longitude)
            put("address", session.address)
            put("parkedTime", session.parkedTimeMillis)
            put("notes", session.notes)
            put("radius", session.alertRadiusMeters.toDouble())
            put("timerDuration", session.timerDurationMinutes)
            put("timerEnd", session.timerEndMillis)
            put("timerAlertLead", session.timerAlertLeadMinutes)
            put("timerAlertTriggered", session.timerAlertTriggered)
        }
        prefs.edit().putString("current_session", json.toString()).apply()
    }
}
