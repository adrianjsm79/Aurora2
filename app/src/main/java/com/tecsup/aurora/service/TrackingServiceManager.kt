package com.tecsup.aurora.service

import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Clase 'Helper' para iniciar y detener el TrackingService
 * de forma limpia desde cualquier parte de la app (ej. ViewModels).
 */
class TrackingServiceManager(private val context: Context) {

    fun startTracking() {
        // (Aquí deberías re-confirmar que los permisos están dados)
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START_SERVICE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopTracking() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP_SERVICE
        }

        // No importa la versión de Android, 'startService' es correcto para detenerlo
        context.startService(intent)
    }
}