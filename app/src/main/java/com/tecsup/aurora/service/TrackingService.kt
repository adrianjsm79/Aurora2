package com.tecsup.aurora.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.data.repository.AuthRepository
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

    // Inyectamos el repositorio desde MyApplication
    private val authRepository: AuthRepository by lazy {
        (application as MyApplication).authRepository
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
                startTracking()
            }
            ACTION_STOP_SERVICE -> {
                Log.d("TrackingService", "Deteniendo servicio...")
                stopTracking()
            }
        }
        return START_STICKY
    }

    private fun startTracking() {
        // 1. CREA LA NOTIFICACIÓN
        // Pasamos 'this' (el servicio) para que el helper pueda crear el PendingIntent
        val notification = NotificationHelper.createNotification(this)

        // 2. INICIA EL SERVICIO EN PRIMER PLANO
        // Esto es OBLIGATORIO. Si no lo haces, la app crasheará.
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)

        // 3. CONFIGURA LA UBICACIÓN
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000 // 10 segundos
        ).build()

        // 4. INICIA LAS ACTUALIZACIONES
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("TrackingService", "Faltan permisos de ubicación", e)
            stopTracking() // Detener si no hay permisos
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("TrackingService", "Nueva ubicación: ${location.latitude}, ${location.longitude}")

                    // 5. ENVÍA LA UBICACIÓN AL BACKEND
                    scope.launch {
                        try {
                            // (Aquí usaremos tu AuthRepository, pero
                            // necesitarás un LocationRepository separado para esto)

                            // val token = authRepository.getToken()
                            // val deviceId = DeviceHelper.getDeviceIdentifier(this@TrackingService)
                            // locationRepository.sendLocation(token, deviceId, location.latitude, ...)

                        } catch (e: Exception) {
                            Log.e("TrackingService", "Error al enviar ubicación", e)
                        }
                    }
                }
            }
        }
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(true) // true = elimina la notificación
        stopSelf() // Detiene el servicio
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // Cancela todas las corrutinas
    }

    override fun onBind(intent: Intent?): IBinder? = null
}