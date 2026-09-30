package ar.edu.unlp.semalert

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ZoneStore(context: Context) {
    private val prefs = context.getSharedPreferences("zones", Context.MODE_PRIVATE)

    fun getZones(): MutableList<Zone> {
        val raw = prefs.getString("data", "[]") ?: "[]"
        val array = JSONArray(raw)
        val result = mutableListOf<Zone>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result += Zone(
                id = o.getString("id"),
                name = o.optString("name", "Zona SEM"),
                latitude = o.getDouble("lat"),
                longitude = o.getDouble("lon"),
                radiusMeters = o.optDouble("radius", 100.0).toFloat()
            )
        }
        return result
    }

    fun add(name: String, lat: Double, lon: Double, radius: Float): Zone {
        val zones = getZones()
        val zone = Zone(UUID.randomUUID().toString(), name.ifBlank { "Zona SEM" }, lat, lon, radius)
        zones += zone
        save(zones)
        return zone
    }

    fun clear() {
        prefs.edit().putString("data", "[]").apply()
    }

    private fun save(zones: List<Zone>) {
        val array = JSONArray()
        zones.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("lat", it.latitude)
                    .put("lon", it.longitude)
                    .put("radius", it.radiusMeters)
            )
        }
        prefs.edit().putString("data", array.toString()).apply()
    }
}
