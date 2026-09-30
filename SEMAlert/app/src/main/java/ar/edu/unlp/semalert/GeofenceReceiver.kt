package ar.edu.unlp.semalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val settings = AppSettingsStore(context)
        val transition = event.geofenceTransition
        val triggeringGeofences = event.triggeringGeofences.orEmpty()

        for (geofence in triggeringGeofences) {
            val id = geofence.requestId

            if (id == GeofenceManager.GEOFENCE_PARKED_CAR_ID) {
                if (transition == Geofence.GEOFENCE_TRANSITION_EXIT && settings.alertOnExit) {
                    NotificationHelper.showAlert(
                        context,
                        "🚗 Te alejaste de tu auto",
                        "Saliste del radio de alerta de tu vehículo. Verificá si tenés activo el estacionamiento medido (SEM).",
                        AlertType.GEOFENCE_EXIT
                    )
                } else if (transition == Geofence.GEOFENCE_TRANSITION_ENTER && settings.alertOnEnter) {
                    NotificationHelper.showAlert(
                        context,
                        "🚗 Cerca de tu auto",
                        "Estás de regreso en el perímetro de tu vehículo estacionado.",
                        AlertType.GEOFENCE_ENTER
                    )
                }
            } else {
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER && settings.alertOnEnter) {
                    val store = ZoneStore(context)
                    val zone = store.getZones().firstOrNull { it.id == id }
                    val name = zone?.name ?: "Zona SEM"
                    NotificationHelper.showAlert(
                        context,
                        "⚠️ Zona de Estacionamiento Medido",
                        "Entraste en $name. Recordá iniciar el SEM para evitar multas de control.",
                        AlertType.GEOFENCE_ENTER
                    )
                }
            }
        }
    }
}
