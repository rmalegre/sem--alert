package ar.edu.unlp.semalert

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var parkingStore: ParkingStore
    private lateinit var zoneStore: ZoneStore
    private lateinit var settingsStore: AppSettingsStore
    private lateinit var logStore: NotificationLogStore
    private lateinit var geofenceManager: GeofenceManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Navigation & Panels
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var panelMap: View
    private lateinit var panelParking: View
    private lateinit var panelTimer: View
    private lateinit var panelNotifications: View
    private lateinit var panelSettings: View
    private lateinit var headerStatusBadge: TextView

    // Tab 1: Map & Location
    private lateinit var mapWebView: WebView
    private lateinit var txtMapAddress: TextView
    private lateinit var txtMapAccuracy: TextView
    private lateinit var btnRefreshLocation: ImageButton
    private lateinit var fabCenterUser: FloatingActionButton
    private lateinit var fabCenterCar: FloatingActionButton
    private lateinit var txtRadiusLabel: TextView
    private lateinit var txtCarDistanceMap: TextView
    private lateinit var radiusSeekBar: SeekBar
    private lateinit var btnMapQuickPark: MaterialButton

    // Tab 2: Parking
    private lateinit var txtParkingHeroTitle: TextView
    private lateinit var txtParkingStatusBadge: TextView
    private lateinit var txtParkingAddress: TextView
    private lateinit var txtParkingDistance: TextView
    private lateinit var txtParkingTime: TextView
    private lateinit var btnParkHere: MaterialButton
    private lateinit var layoutParkedActions: LinearLayout
    private lateinit var btnNavigateToCar: MaterialButton
    private lateinit var btnClearParking: MaterialButton
    private lateinit var cardParkingNotes: View
    private lateinit var inputParkingNotes: EditText
    private lateinit var btnSaveNotes: MaterialButton
    private lateinit var zonesListContainer: LinearLayout

    // Tab 3: Timer
    private lateinit var txtTimerDisplay: TextView
    private lateinit var txtTimerSubtext: TextView
    private lateinit var btnAdd15m: MaterialButton
    private lateinit var btnAdd30m: MaterialButton
    private lateinit var btnAdd1h: MaterialButton
    private lateinit var btnAdd2h: MaterialButton
    private lateinit var radioGroupLeadAlert: RadioGroup
    private lateinit var rbLead5: RadioButton
    private lateinit var rbLead10: RadioButton
    private lateinit var rbLead15: RadioButton
    private lateinit var btnCancelTimer: MaterialButton

    // Tab 4: Notifications
    private lateinit var switchAlertExit: SwitchMaterial
    private lateinit var switchAlertEnter: SwitchMaterial
    private lateinit var switchSound: SwitchMaterial
    private lateinit var switchVibration: SwitchMaterial
    private lateinit var btnTestNotification: MaterialButton
    private lateinit var btnClearNotifLogs: Button
    private lateinit var containerNotifLogs: LinearLayout

    // Tab 5: Settings & Diagnostics
    private lateinit var txtStatusLocation: TextView
    private lateinit var btnReqLocation: Button
    private lateinit var txtStatusBackground: TextView
    private lateinit var btnReqBackground: Button
    private lateinit var txtStatusNotification: TextView
    private lateinit var btnReqNotification: Button
    private lateinit var txtStatusBattery: TextView
    private lateinit var btnReqBattery: Button
    private lateinit var inputDefaultRadius: EditText
    private lateinit var inputSemStartHour: EditText
    private lateinit var inputSemEndHour: EditText
    private lateinit var btnSaveSettings: MaterialButton
    private lateinit var btnResetAll: MaterialButton

    // State
    private var lastKnownLocation: Location? = null
    private var isMapReady = false
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimerDisplay()
            timerHandler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val REQ_LOCATION = 100
        private const val REQ_NOTIFICATIONS = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        parkingStore = ParkingStore(this)
        zoneStore = ZoneStore(this)
        settingsStore = AppSettingsStore(this)
        logStore = NotificationLogStore(this)
        geofenceManager = GeofenceManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        NotificationHelper.createChannels(this)

        initViews()
        setupBottomNav()
        setupMapWebView()
        setupParkingTab()
        setupTimerTab()
        setupNotificationsTab()
        setupSettingsTab()

        refreshCurrentLocation()
        syncAllUI()
    }

    override fun onResume() {
        super.onResume()
        timerHandler.post(timerRunnable)
        refreshPermissionsUI()
        refreshCurrentLocation()
        syncAllUI()
    }

    override fun onPause() {
        super.onPause()
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun initViews() {
        bottomNav = findViewById(R.id.bottomNavigation)
        panelMap = findViewById(R.id.panelMap)
        panelParking = findViewById(R.id.panelParking)
        panelTimer = findViewById(R.id.panelTimer)
        panelNotifications = findViewById(R.id.panelNotifications)
        panelSettings = findViewById(R.id.panelSettings)
        headerStatusBadge = findViewById(R.id.headerStatusBadge)

        // Map Views
        mapWebView = findViewById(R.id.mapWebView)
        txtMapAddress = findViewById(R.id.txtMapAddress)
        txtMapAccuracy = findViewById(R.id.txtMapAccuracy)
        btnRefreshLocation = findViewById(R.id.btnRefreshLocation)
        fabCenterUser = findViewById(R.id.fabCenterUser)
        fabCenterCar = findViewById(R.id.fabCenterCar)
        txtRadiusLabel = findViewById(R.id.txtRadiusLabel)
        txtCarDistanceMap = findViewById(R.id.txtCarDistanceMap)
        radiusSeekBar = findViewById(R.id.radiusSeekBar)
        btnMapQuickPark = findViewById(R.id.btnMapQuickPark)

        // Parking Views
        txtParkingHeroTitle = findViewById(R.id.txtParkingHeroTitle)
        txtParkingStatusBadge = findViewById(R.id.txtParkingStatusBadge)
        txtParkingAddress = findViewById(R.id.txtParkingAddress)
        txtParkingDistance = findViewById(R.id.txtParkingDistance)
        txtParkingTime = findViewById(R.id.txtParkingTime)
        btnParkHere = findViewById(R.id.btnParkHere)
        layoutParkedActions = findViewById(R.id.layoutParkedActions)
        btnNavigateToCar = findViewById(R.id.btnNavigateToCar)
        btnClearParking = findViewById(R.id.btnClearParking)
        cardParkingNotes = findViewById(R.id.cardParkingNotes)
        inputParkingNotes = findViewById(R.id.inputParkingNotes)
        btnSaveNotes = findViewById(R.id.btnSaveNotes)
        zonesListContainer = findViewById(R.id.zonesListContainer)

        // Timer Views
        txtTimerDisplay = findViewById(R.id.txtTimerDisplay)
        txtTimerSubtext = findViewById(R.id.txtTimerSubtext)
        btnAdd15m = findViewById(R.id.btnAdd15m)
        btnAdd30m = findViewById(R.id.btnAdd30m)
        btnAdd1h = findViewById(R.id.btnAdd1h)
        btnAdd2h = findViewById(R.id.btnAdd2h)
        radioGroupLeadAlert = findViewById(R.id.radioGroupLeadAlert)
        rbLead5 = findViewById(R.id.rbLead5)
        rbLead10 = findViewById(R.id.rbLead10)
        rbLead15 = findViewById(R.id.rbLead15)
        btnCancelTimer = findViewById(R.id.btnCancelTimer)

        // Notifications Views
        switchAlertExit = findViewById(R.id.switchAlertExit)
        switchAlertEnter = findViewById(R.id.switchAlertEnter)
        switchSound = findViewById(R.id.switchSound)
        switchVibration = findViewById(R.id.switchVibration)
        btnTestNotification = findViewById(R.id.btnTestNotification)
        btnClearNotifLogs = findViewById(R.id.btnClearNotifLogs)
        containerNotifLogs = findViewById(R.id.containerNotifLogs)

        // Settings Views
        txtStatusLocation = findViewById(R.id.txtStatusLocation)
        btnReqLocation = findViewById(R.id.btnReqLocation)
        txtStatusBackground = findViewById(R.id.txtStatusBackground)
        btnReqBackground = findViewById(R.id.btnReqBackground)
        txtStatusNotification = findViewById(R.id.txtStatusNotification)
        btnReqNotification = findViewById(R.id.btnReqNotification)
        txtStatusBattery = findViewById(R.id.txtStatusBattery)
        btnReqBattery = findViewById(R.id.btnReqBattery)
        inputDefaultRadius = findViewById(R.id.inputDefaultRadius)
        inputSemStartHour = findViewById(R.id.inputSemStartHour)
        inputSemEndHour = findViewById(R.id.inputSemEndHour)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        btnResetAll = findViewById(R.id.btnResetAll)
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            panelMap.visibility = if (item.itemId == R.id.nav_map) View.VISIBLE else View.GONE
            panelParking.visibility = if (item.itemId == R.id.nav_parking) View.VISIBLE else View.GONE
            panelTimer.visibility = if (item.itemId == R.id.nav_timer) View.VISIBLE else View.GONE
            panelNotifications.visibility = if (item.itemId == R.id.nav_notifications) View.VISIBLE else View.GONE
            panelSettings.visibility = if (item.itemId == R.id.nav_settings) View.VISIBLE else View.GONE

            if (item.itemId == R.id.nav_map && isMapReady) {
                updateMapMarkers()
            }
            if (item.itemId == R.id.nav_notifications) {
                renderNotificationLogs()
            }
            if (item.itemId == R.id.nav_settings) {
                refreshPermissionsUI()
            }
            true
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupMapWebView() {
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.settings.domStorageEnabled = true
        mapWebView.settings.allowFileAccess = true
        mapWebView.webChromeClient = WebChromeClient()
        mapWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapReady = true
                updateMapMarkers()
            }
        }

        mapWebView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onMapReady() {
                runOnUiThread {
                    isMapReady = true
                    updateMapMarkers()
                }
            }

            @JavascriptInterface
            fun onMapClick(lat: Double, lng: Double) {
                runOnUiThread {
                    showQuickParkDialog(lat, lng)
                }
            }
        }, "AndroidBridge")

        mapWebView.loadUrl("file:///android_asset/map.html")

        btnRefreshLocation.setOnClickListener {
            refreshCurrentLocation()
            Toast.makeText(this, "Actualizando ubicación...", Toast.LENGTH_SHORT).show()
        }

        fabCenterUser.setOnClickListener {
            mapWebView.evaluateJavascript("centerOnUser()", null)
        }

        fabCenterCar.setOnClickListener {
            mapWebView.evaluateJavascript("centerOnCar()", null)
        }

        // Radius Slider: 50m to 1000m
        val currentRadius = parkingStore.getParking()?.alertRadiusMeters ?: settingsStore.defaultRadiusMeters
        val progress = (currentRadius - 50).toInt().coerceIn(0, 950)
        radiusSeekBar.progress = progress
        txtRadiusLabel.text = "📏 Radio de alerta: ${currentRadius.toInt()} m"

        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val radius = (progress + 50).toFloat()
                txtRadiusLabel.text = "📏 Radio de alerta: ${radius.toInt()} m"
                if (isMapReady) {
                    mapWebView.evaluateJavascript("updateRadius($radius)", null)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val radius = (radiusSeekBar.progress + 50).toFloat()
                if (parkingStore.getParking() != null) {
                    parkingStore.updateRadius(radius)
                    geofenceManager.syncAll()
                } else {
                    settingsStore.defaultRadiusMeters = radius
                }
                syncAllUI()
            }
        })

        btnMapQuickPark.setOnClickListener {
            parkAtCurrentLocation()
        }
    }

    private fun setupParkingTab() {
        btnParkHere.setOnClickListener {
            parkAtCurrentLocation()
        }

        btnClearParking.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("¿Liberar estacionamiento?")
                .setMessage("Se eliminará la ubicación del auto y se cancelarán la geocerca de alerta y el temporizador.")
                .setPositiveButton("Sí, ya me voy") { _, _ ->
                    clearParkingSession()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnNavigateToCar.setOnClickListener {
            val parking = parkingStore.getParking()
            if (parking != null) {
                val uri = Uri.parse("google.navigation:q=${parking.latitude},${parking.longitude}&mode=w")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    // Fallback to generic geo URI
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${parking.latitude},${parking.longitude}(Mi Auto)"))
                    startActivity(fallback)
                }
            }
        }

        btnSaveNotes.setOnClickListener {
            val notes = inputParkingNotes.text.toString().trim()
            parkingStore.updateNotes(notes)
            Toast.makeText(this, "Notas guardadas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTimerTab() {
        btnAdd15m.setOnClickListener { addTimerMinutes(15) }
        btnAdd30m.setOnClickListener { addTimerMinutes(30) }
        btnAdd1h.setOnClickListener { addTimerMinutes(60) }
        btnAdd2h.setOnClickListener { addTimerMinutes(120) }

        radioGroupLeadAlert.setOnCheckedChangeListener { _, checkedId ->
            val lead = when (checkedId) {
                R.id.rbLead5 -> 5
                R.id.rbLead15 -> 15
                else -> 10
            }
            settingsStore.leadAlertMinutes = lead
            val parking = parkingStore.getParking()
            if (parking != null && parking.isTimerActive) {
                parkingStore.updateTimer(parking.timerEndMillis, parking.timerDurationMinutes, lead)
                ParkingTimerReceiver.scheduleTimer(this, parking.timerEndMillis, lead)
            }
        }

        btnCancelTimer.setOnClickListener {
            val parking = parkingStore.getParking()
            if (parking != null) {
                parkingStore.updateTimer(0L, 0)
                ParkingTimerReceiver.cancelTimer(this)
                syncAllUI()
                Toast.makeText(this, "Temporizador cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNotificationsTab() {
        switchAlertExit.isChecked = settingsStore.alertOnExit
        switchAlertEnter.isChecked = settingsStore.alertOnEnter
        switchSound.isChecked = settingsStore.soundEnabled
        switchVibration.isChecked = settingsStore.vibrationEnabled

        switchAlertExit.setOnCheckedChangeListener { _, isChecked -> settingsStore.alertOnExit = isChecked }
        switchAlertEnter.setOnCheckedChangeListener { _, isChecked -> settingsStore.alertOnEnter = isChecked }
        switchSound.setOnCheckedChangeListener { _, isChecked -> settingsStore.soundEnabled = isChecked }
        switchVibration.setOnCheckedChangeListener { _, isChecked -> settingsStore.vibrationEnabled = isChecked }

        btnTestNotification.setOnClickListener {
            if (!hasNotificationPermission()) {
                requestNotificationPermission()
            } else {
                NotificationHelper.showTestNotification(this)
                renderNotificationLogs()
            }
        }

        btnClearNotifLogs.setOnClickListener {
            logStore.clear()
            renderNotificationLogs()
        }
    }

    private fun setupSettingsTab() {
        btnReqLocation.setOnClickListener { requestLocationPermission() }
        btnReqBackground.setOnClickListener { openAppSettings() }
        btnReqNotification.setOnClickListener { requestNotificationPermission() }
        btnReqBattery.setOnClickListener { requestIgnoreBatteryOptimizations() }

        inputDefaultRadius.setText(settingsStore.defaultRadiusMeters.toInt().toString())
        inputSemStartHour.setText(settingsStore.semStartHour.toString())
        inputSemEndHour.setText(settingsStore.semEndHour.toString())

        btnSaveSettings.setOnClickListener {
            val radius = inputDefaultRadius.text.toString().toFloatOrNull() ?: 100f
            val startHour = inputSemStartHour.text.toString().toIntOrNull() ?: 7
            val endHour = inputSemEndHour.text.toString().toIntOrNull() ?: 20

            settingsStore.defaultRadiusMeters = radius.coerceIn(50f, 1000f)
            settingsStore.semStartHour = startHour.coerceIn(0, 23)
            settingsStore.semEndHour = endHour.coerceIn(0, 23)

            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
            syncAllUI()
        }

        btnResetAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("¿Restablecer todo?")
                .setMessage("Se borrarán las configuraciones, zonas, estacionamiento activo e historial.")
                .setPositiveButton("Restablecer") { _, _ ->
                    parkingStore.clearParking()
                    zoneStore.clear()
                    logStore.clear()
                    settingsStore.resetDefaults()
                    geofenceManager.removeAll()
                    ParkingTimerReceiver.cancelTimer(this)
                    NotificationHelper.cancelParkingStatus(this)
                    syncAllUI()
                    renderNotificationLogs()
                    Toast.makeText(this, "Todo ha sido restablecido", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    // ================= LOCATION ENGINE =================
    private fun refreshCurrentLocation() {
        if (!hasLocationPermission()) {
            txtMapAddress.text = "📍 Permiso de GPS requerido"
            txtMapAccuracy.text = "Tocá para conceder permiso"
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    onLocationUpdated(loc)
                }
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateDistanceMeters(5f)
                .build()

            fusedLocationClient.requestLocationUpdates(
                request,
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val loc = result.lastLocation ?: return
                        onLocationUpdated(loc)
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                },
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Ignored
        }
    }

    private fun onLocationUpdated(location: Location) {
        lastKnownLocation = location
        txtMapAccuracy.text = "Precisión: ±${location.accuracy.toInt()} m"

        // Reverse geocoding on worker thread
        resolveAddress(location.latitude, location.longitude) { address ->
            runOnUiThread {
                txtMapAddress.text = "📍 $address"
            }
        }

        if (isMapReady) {
            mapWebView.evaluateJavascript("setUserPosition(${location.latitude}, ${location.longitude}, ${location.accuracy})", null)
        }

        updateParkingDistanceDisplay()
    }

    private fun resolveAddress(lat: Double, lon: Double, callback: (String) -> Unit) {
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val list = geocoder.getFromLocation(lat, lon, 1)
                if (!list.isNullOrEmpty()) {
                    val addr = list[0]
                    val thoroughfare = addr.thoroughfare
                    val subThoroughfare = addr.subThoroughfare
                    val locality = addr.locality ?: addr.subAdminArea ?: ""

                    val formatted = when {
                        thoroughfare != null && subThoroughfare != null -> "$thoroughfare $subThoroughfare, $locality"
                        thoroughfare != null -> "$thoroughfare, $locality"
                        else -> addr.getAddressLine(0) ?: "$lat, $lon"
                    }
                    callback(formatted)
                    return@Thread
                }
            } catch (e: Exception) {
                // Network or Geocoder not available
            }
            callback(String.format(Locale.US, "%.5f, %.5f", lat, lon))
        }.start()
    }

    // ================= PARKING MANAGEMENT =================
    private fun parkAtCurrentLocation() {
        val loc = lastKnownLocation
        if (loc == null) {
            if (!hasLocationPermission()) {
                requestLocationPermission()
            } else {
                Toast.makeText(this, "Obteniendo señal GPS... Probá en un segundo.", Toast.LENGTH_SHORT).show()
                refreshCurrentLocation()
            }
            return
        }

        resolveAddress(loc.latitude, loc.longitude) { address ->
            runOnUiThread {
                val radius = (radiusSeekBar.progress + 50).toFloat()
                parkingStore.saveParking(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    address = address,
                    radius = radius,
                    timerDurationMinutes = 60,
                    timerEndMillis = System.currentTimeMillis() + (60 * 60 * 1000L),
                    timerAlertLeadMinutes = settingsStore.leadAlertMinutes
                )

                // Schedule geofence and timer
                geofenceManager.syncAll()
                ParkingTimerReceiver.scheduleTimer(
                    this,
                    System.currentTimeMillis() + (60 * 60 * 1000L),
                    settingsStore.leadAlertMinutes
                )

                NotificationHelper.showAlert(
                    this,
                    "🚗 Auto Estacionado",
                    "Estacionado en $address. Radio de alerta: ${radius.toInt()} m. Temporizador iniciado por 60 min.",
                    AlertType.PARKED
                )
                NotificationHelper.showParkingStatus(this, address, "60 min restantes")

                syncAllUI()
                Toast.makeText(this, "¡Auto estacionado y alerta activada!", Toast.LENGTH_SHORT).show()

                if (isMapReady) {
                    mapWebView.evaluateJavascript("setParkedPosition(${loc.latitude}, ${loc.longitude}, $radius)", null)
                    mapWebView.evaluateJavascript("centerOnCar()", null)
                }
            }
        }
    }

    private fun showQuickParkDialog(lat: Double, lng: Double) {
        AlertDialog.Builder(this)
            .setTitle("🚗 Marcar auto aquí")
            .setMessage("¿Deseas fijar la ubicación del auto estacionado en este punto del mapa?")
            .setPositiveButton("Estacionar aquí") { _, _ ->
                resolveAddress(lat, lng) { address ->
                    runOnUiThread {
                        val radius = (radiusSeekBar.progress + 50).toFloat()
                        parkingStore.saveParking(
                            latitude = lat,
                            longitude = lng,
                            address = address,
                            radius = radius
                        )
                        geofenceManager.syncAll()
                        syncAllUI()
                        Toast.makeText(this, "Estacionamiento fijado en $address", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clearParkingSession() {
        parkingStore.clearParking()
        geofenceManager.syncAll()
        ParkingTimerReceiver.cancelTimer(this)
        NotificationHelper.cancelParkingStatus(this)

        if (isMapReady) {
            mapWebView.evaluateJavascript("clearParkedPosition()", null)
        }

        syncAllUI()
        Toast.makeText(this, "Estacionamiento liberado", Toast.LENGTH_SHORT).show()
    }

    // ================= TIMER MANAGEMENT =================
    private fun addTimerMinutes(minutesToAdd: Int) {
        val parking = parkingStore.getParking()
        val now = System.currentTimeMillis()

        val baseEnd = if (parking != null && parking.timerEndMillis > now) {
            parking.timerEndMillis
        } else {
            now
        }

        val newEndMillis = baseEnd + (minutesToAdd * 60 * 1000L)
        val totalMinutes = ((newEndMillis - now) / 60000L).toInt()
        val lead = settingsStore.leadAlertMinutes

        if (parking != null) {
            parkingStore.updateTimer(newEndMillis, totalMinutes, lead)
        } else {
            // Virtual timer without vehicle spot
            val loc = lastKnownLocation
            val lat = loc?.latitude ?: -27.367
            val lon = loc?.longitude ?: -55.896
            parkingStore.saveParking(
                latitude = lat,
                longitude = lon,
                address = "Estacionamiento SEM",
                timerDurationMinutes = totalMinutes,
                timerEndMillis = newEndMillis,
                timerAlertLeadMinutes = lead
            )
        }

        ParkingTimerReceiver.scheduleTimer(this, newEndMillis, lead)
        syncAllUI()
        Toast.makeText(this, "+$minutesToAdd min agregados al temporizador", Toast.LENGTH_SHORT).show()
    }

    private fun updateTimerDisplay() {
        val parking = parkingStore.getParking()
        if (parking == null || parking.timerEndMillis <= 0L) {
            txtTimerDisplay.text = "00:00:00"
            txtTimerSubtext.text = "Sin temporizador activo"
            btnCancelTimer.visibility = View.GONE
            return
        }

        val diff = parking.timerEndMillis - System.currentTimeMillis()
        btnCancelTimer.visibility = View.VISIBLE

        if (diff <= 0) {
            txtTimerDisplay.text = "00:00:00"
            txtTimerSubtext.text = "⚠️ Tiempo de estacionamiento vencido"
            txtTimerDisplay.setTextColor(ContextCompat.getColor(this, R.color.accent_danger))
        } else {
            val hours = (diff / (1000 * 60 * 60)).toInt()
            val minutes = ((diff / (1000 * 60)) % 60).toInt()
            val seconds = ((diff / 1000) % 60).toInt()

            txtTimerDisplay.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            txtTimerDisplay.setTextColor(ContextCompat.getColor(this, R.color.primary))

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val expirationHour = timeFormat.format(Date(parking.timerEndMillis))
            txtTimerSubtext.text = "Vence a las $expirationHour hs (${parking.timerAlertLeadMinutes}m aviso previo)"
        }
    }

    // ================= UI SYNCHRONIZATION =================
    private fun syncAllUI() {
        val parking = parkingStore.getParking()

        if (parking != null) {
            headerStatusBadge.text = "🟢 Estacionado"
            headerStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            headerStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.secondary))

            // Parking Tab UI
            txtParkingHeroTitle.text = "🚗 Auto Estacionado"
            txtParkingStatusBadge.text = "Activo"
            txtParkingStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            txtParkingStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.secondary))
            txtParkingAddress.text = parking.address

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            txtParkingTime.text = "Estacionado a las ${timeFormat.format(Date(parking.parkedTimeMillis))}"

            btnParkHere.visibility = View.GONE
            layoutParkedActions.visibility = View.VISIBLE
            cardParkingNotes.visibility = View.VISIBLE
            inputParkingNotes.setText(parking.notes)

            // Map FAB
            fabCenterCar.visibility = View.VISIBLE
            btnMapQuickPark.text = "🚗 Cambiar a ubicación actual"

            // Radius Slider
            txtRadiusLabel.text = "📏 Radio de alerta: ${parking.alertRadiusMeters.toInt()} m"
            radiusSeekBar.progress = (parking.alertRadiusMeters - 50).toInt().coerceIn(0, 950)
        } else {
            headerStatusBadge.text = "⚪ Libre"
            headerStatusBadge.setBackgroundResource(R.drawable.bg_badge_inactive)
            headerStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            // Parking Tab UI
            txtParkingHeroTitle.text = "🚗 Estado del Vehículo"
            txtParkingStatusBadge.text = "No estacionado"
            txtParkingStatusBadge.setBackgroundResource(R.drawable.bg_badge_inactive)
            txtParkingStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            txtParkingAddress.text = "No registraste ningún auto estacionado actualmente."
            txtParkingDistance.text = ""
            txtParkingTime.text = ""

            btnParkHere.visibility = View.VISIBLE
            layoutParkedActions.visibility = View.GONE
            cardParkingNotes.visibility = View.GONE

            // Map FAB
            fabCenterCar.visibility = View.GONE
            btnMapQuickPark.text = "🚗 Estoy estacionado aquí"

            // Radius
            val defaultR = settingsStore.defaultRadiusMeters
            txtRadiusLabel.text = "📏 Radio de alerta: ${defaultR.toInt()} m"
            radiusSeekBar.progress = (defaultR - 50).toInt().coerceIn(0, 950)
        }

        updateParkingDistanceDisplay()
        updateMapMarkers()
        renderCustomZones()
    }

    private fun updateParkingDistanceDisplay() {
        val parking = parkingStore.getParking()
        val loc = lastKnownLocation

        if (parking != null && loc != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                loc.latitude, loc.longitude,
                parking.latitude, parking.longitude,
                results
            )
            val dist = results[0].toInt()

            val text = "🚶 Estás a $dist metros de tu auto"
            txtParkingDistance.text = text
            txtCarDistanceMap.text = "$dist m al auto"
        } else {
            txtParkingDistance.text = ""
            txtCarDistanceMap.text = ""
        }
    }

    private fun updateMapMarkers() {
        if (!isMapReady) return

        val loc = lastKnownLocation
        if (loc != null) {
            mapWebView.evaluateJavascript("setUserPosition(${loc.latitude}, ${loc.longitude}, ${loc.accuracy})", null)
        }

        val parking = parkingStore.getParking()
        if (parking != null) {
            mapWebView.evaluateJavascript("setParkedPosition(${parking.latitude}, ${parking.longitude}, ${parking.alertRadiusMeters})", null)
        } else {
            mapWebView.evaluateJavascript("clearParkedPosition()", null)
        }
    }

    private fun renderCustomZones() {
        zonesListContainer.removeAllViews()
        val zones = zoneStore.getZones()
        if (zones.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No tenés zonas fijas adicionales configuradas."
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
                textSize = 13f
            }
            zonesListContainer.addView(empty)
            return
        }

        zones.forEach { zone ->
            val item = TextView(this).apply {
                text = "📍 ${zone.name} (Radio ${zone.radiusMeters.toInt()}m)"
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                textSize = 14f
                setPadding(0, 8, 0, 8)
            }
            zonesListContainer.addView(item)
        }
    }

    private fun renderNotificationLogs() {
        containerNotifLogs.removeAllViews()
        val logs = logStore.getLogs()

        if (logs.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No hay eventos registrados recientemente."
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
                textSize = 13f
                setPadding(0, 12, 0, 12)
            }
            containerNotifLogs.addView(empty)
            return
        }

        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        logs.forEach { log ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_badge_inactive)
                setPadding(12, 10, 12, 10)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 8)
                layoutParams = params
            }

            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val title = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = log.title
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            }

            val time = TextView(this).apply {
                text = sdf.format(Date(log.timestamp))
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            }

            header.addView(title)
            header.addView(time)

            val msg = TextView(this).apply {
                text = log.message
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                setPadding(0, 4, 0, 0)
            }

            card.addView(header)
            card.addView(msg)
            containerNotifLogs.addView(card)
        }
    }

    // ================= PERMISSIONS & DIAGNOSTICS =================
    private fun refreshPermissionsUI() {
        // 1. Fine Location
        val hasLoc = hasLocationPermission()
        txtStatusLocation.text = if (hasLoc) "📍 Ubicación precisa: Concedida 🟢" else "📍 Ubicación precisa: Requerida 🔴"
        btnReqLocation.visibility = if (hasLoc) View.GONE else View.VISIBLE

        // 2. Background Location
        val hasBg = hasBackgroundLocationPermission()
        txtStatusBackground.text = if (hasBg) "🌐 Segundo plano: Permitido todo el tiempo 🟢" else "🌐 Segundo plano: Solo en uso / No otorgado ⚠️"
        btnReqBackground.visibility = if (hasBg) View.GONE else View.VISIBLE

        // 3. Notifications
        val hasNotif = hasNotificationPermission()
        txtStatusNotification.text = if (hasNotif) "🔔 Notificaciones: Activadas 🟢" else "🔔 Notificaciones: Desactivadas 🔴"
        btnReqNotification.visibility = if (hasNotif) View.GONE else View.VISIBLE

        // 4. Battery Optimization
        val isIgnoringBattery = isIgnoringBatteryOptimizations()
        txtStatusBattery.text = if (isIgnoringBattery) "🔋 Batería: Sin restricciones 🟢" else "🔋 Batería: Optimización activa (puede suspender alertas) ⚠️"
        btnReqBattery.visibility = if (isIgnoringBattery) View.GONE else View.VISIBLE
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQ_LOCATION
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_NOTIFICATIONS
            )
        } else {
            Toast.makeText(this, "Las notificaciones están disponibles de fábrica.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        startActivity(intent)
        Toast.makeText(this, "En 'Permisos' -> 'Ubicación', elegí 'Permitir todo el tiempo'.", Toast.LENGTH_LONG).show()
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshPermissionsUI()
        if (requestCode == REQ_LOCATION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            refreshCurrentLocation()
            geofenceManager.syncAll()
        }
    }
}
