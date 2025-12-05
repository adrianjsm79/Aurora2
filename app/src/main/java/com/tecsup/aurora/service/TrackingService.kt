package com.tecsup.aurora.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.data.repository.LocationRepository
import com.tecsup.aurora.data.repository.SettingsRepository
import com.tecsup.aurora.utils.DeviceHelper
import com.tecsup.aurora.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TrackingService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Inyecciones desde MyApplication
    private val authRepository: AuthRepository by lazy {
        (application as MyApplication).authRepository
    }
    private val locationRepository: LocationRepository by lazy {
        (application as MyApplication).locationRepository
    }
    private val settingsRepository: SettingsRepository by lazy {
        (application as MyApplication).settingsRepository
    }

    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                Log.d("TrackingService", "Iniciando servicio...")

                val notification = NotificationHelper.createNotification(this)
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)

                initializeTracking()
            }
            ACTION_STOP_SERVICE -> {
                Log.d("TrackingService", "Deteniendo servicio...")
                stopTracking()
            }
        }
        return START_STICKY
    }

    private fun initializeTracking() {
        scope.launch {
            try {
                val token = authRepository.getToken()

                if (token != null) {
                    locationRepository.connect(token)
                    startLocationUpdates()

                    // CONFIRMACIÓN: El servicio está corriendo, actualizamos persistencia a TRUE
                    settingsRepository.saveTrackingState(true)
                } else {
                    Log.e("TrackingService", "No hay token. Deteniendo.")
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e("TrackingService", "Error al inicializar", e)
                stopSelf()
            }
        }
    }

    private fun startLocationUpdates() {
        val intervalSeconds = settingsRepository.getTrackingInterval()
        val intervalMillis = intervalSeconds * 1000L

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        ).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("TrackingService", "Faltan permisos de ubicación", e)
            stopTracking()
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("TrackingService", "Ubicación: ${location.latitude}, ${location.longitude}")

                    scope.launch {
                        try {
                            val deviceId = DeviceHelper.getDeviceIdentifier(this@TrackingService)
                            locationRepository.sendLocation(
                                deviceId = deviceId,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = if (location.hasAccuracy()) location.accuracy else null
                            )
                        } catch (e: Exception) {
                            Log.e("TrackingService", "Error enviando ubicación", e)
                        }
                    }
                }
            }
        }
    }

    private fun stopTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationRepository.disconnect()
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.e("TrackingService", "Error al detener", e)
        } finally {
            //Si paramos, actualizamos persistencia a FALSE
            settingsRepository.saveTrackingState(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //Si el sistema mata el servicio, marcamos como inactivo
        settingsRepository.saveTrackingState(false)
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}