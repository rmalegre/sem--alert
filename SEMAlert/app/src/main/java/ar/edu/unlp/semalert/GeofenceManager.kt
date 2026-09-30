package ar.edu.unlp.semalert

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.concurrent.TimeUnit

class GeofenceManager(private val context: Context) {

    companion object {
        const val GEOFENCE_PARKED_CAR_ID = "PARKED_CAR"
    }

    private val client: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun syncAll(
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        if (!hasLocationPermission()) {
            onError(SecurityException("Falta permiso de ubicación"))
            return
        }

        val geofenceList = mutableListOf<Geofence>()

        // 1. Parking car geofence
        val parking = ParkingStore(context).getParking()
        if (parking != null) {
            val radius = parking.alertRadiusMeters.coerceIn(50f, 1000f)
            geofenceList.add(
                Geofence.Builder()
                    .setRequestId(GEOFENCE_PARKED_CAR_ID)
                    .setCircularRegion(parking.latitude, parking.longitude, radius)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setExpirationDuration(TimeUnit.DAYS.toMillis(30))
                    .build()
            )
        }

        // 2. Custom SEM zones
        val zones = ZoneStore(context).getZones()
        zones.forEach { zone ->
            val radius = zone.radiusMeters.coerceIn(50f, 1000f)
            geofenceList.add(
                Geofence.Builder()
                    .setRequestId(zone.id)
                    .setCircularRegion(zone.latitude, zone.longitude, radius)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setExpirationDuration(TimeUnit.DAYS.toMillis(30))
                    .build()
            )
        }

        try {
            client.removeGeofences(pendingIntent).addOnCompleteListener {
                if (geofenceList.isEmpty()) {
                    onSuccess()
                    return@addOnCompleteListener
                }

                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(geofenceList)
                    .build()

                try {
                    client.addGeofences(request, pendingIntent)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onError(it) }
                } catch (e: SecurityException) {
                    onError(e)
                }
            }
        } catch (e: SecurityException) {
            onError(e)
        }
    }

    fun removeAll(onDone: () -> Unit = {}) {
        try {
            client.removeGeofences(pendingIntent).addOnCompleteListener { onDone() }
        } catch (e: SecurityException) {
            onDone()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }
}
