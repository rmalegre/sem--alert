package ar.edu.unlp.semalert

import android.content.Context

class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var defaultRadiusMeters: Float
        get() = prefs.getFloat("default_radius", 100f)
        set(value) = prefs.edit().putFloat("default_radius", value).apply()

    var leadAlertMinutes: Int
        get() = prefs.getInt("lead_alert_minutes", 10)
        set(value) = prefs.edit().putInt("lead_alert_minutes", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("vibration_enabled", value).apply()

    var alertOnExit: Boolean
        get() = prefs.getBoolean("alert_on_exit", true)
        set(value) = prefs.edit().putBoolean("alert_on_exit", value).apply()

    var alertOnEnter: Boolean
        get() = prefs.getBoolean("alert_on_enter", true)
        set(value) = prefs.edit().putBoolean("alert_on_enter", value).apply()

    var semStartHour: Int
        get() = prefs.getInt("sem_start_hour", 7)
        set(value) = prefs.edit().putInt("sem_start_hour", value).apply()

    var semEndHour: Int
        get() = prefs.getInt("sem_end_hour", 20)
        set(value) = prefs.edit().putInt("sem_end_hour", value).apply()

    fun resetDefaults() {
        prefs.edit().clear().apply()
    }
}
