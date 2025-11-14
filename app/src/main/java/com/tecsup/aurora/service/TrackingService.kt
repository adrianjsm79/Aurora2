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

    // --- 3. INYECTA EL REPOSITORIO DE UBICACIÓN ---
    private val locationRepository: LocationRepository by lazy {
        (application as MyApplication).locationRepository
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

                // --- 4. CONECTA EL WEBSOCKET ANTES DE INICIAR ---
                // (Necesitamos el token, que es una llamada síncrona a Realm)
                scope.launch {
                    // (Asegúrate de que authRepository.getTokenFromRealm() exista)
                    val token = authRepository.getToken()

                    if (token != null) {
                        // Conecta el WebSocket
                        locationRepository.connect(token)
                        // Ahora sí, inicia el rastreo GPS
                        startTracking()
                    } else {
                        Log.e("TrackingService", "No hay token, no se puede iniciar el WS. Deteniendo.")
                        stopSelf()
                    }
                }
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
        val notification = NotificationHelper.createNotification(this)

        // 2. INICIA EL SERVICIO EN PRIMER PLANO
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)

        // 3. CONFIGURA LA UBICACIÓN
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000 // 10 segundos
        ).build()

        // 4. INICIA LAS ACTUALIZACIONES
        try {
            // Esta llamada debe hacerse en el hilo principal
            // Looper.getMainLooper() se asegura de eso.
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

                    // --- 5. ¡AQUÍ ESTÁ EL ARREGLO! ---
                    // ENVÍA LA UBICACIÓN AL BACKEND
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
                            Log.e("TrackingService", "Error al enviar ubicación", e)
                        }
                    }
                }
            }
        }
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        locationRepository.disconnect() // --- 6. DESCONECTA EL WEBSOCKET ---
        stopForeground(true) // true = elimina la notificación
        stopSelf() // Detiene el servicio
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // Cancela todas las corrutinas
    }

    override fun onBind(intent: Intent?): IBinder? = null
}