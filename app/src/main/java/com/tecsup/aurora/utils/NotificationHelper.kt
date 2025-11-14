package com.tecsup.aurora.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.res.colorResource
import androidx.core.app.NotificationCompat
import com.tecsup.aurora.R
import com.tecsup.aurora.service.TrackingService

object NotificationHelper {

    const val NOTIFICATION_ID = 101
    private const val CHANNEL_ID = "AURORA_TRACKING_CHANNEL"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Canal de Rastreo de Aurora",
                NotificationManager.IMPORTANCE_LOW // Baja importancia para que no suene
            ).apply {
                description = "Notificación persistente de rastreo de ubicación"

                // --- TU REQUISITO DE SEGURIDAD ---
                // Oculta la notificación en la pantalla de bloqueo
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun createNotification(context: Context): Notification {
        // --- TU REQUISITO DE "PARAR" ---
        // 1. Crea un Intent que llame a nuestro TrackingService
        val stopServiceIntent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP_SERVICE
        }

        // 2. Crea el PendingIntent (la "acción pendiente")
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopServiceIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Construye la notificación
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Aurora")
            .setContentText("Enviando tu ubicación en tiempo real")
            .setSmallIcon(R.drawable.ic_location) // Asegúrate de tener este icono
            .setOngoing(true) // Hace que no se pueda descartar deslizándola
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Doble seguridad
            .addAction(
                R.drawable.ic_stop,  // Un icono para "Parar"
                "Parar Localización", // El texto del botón
                stopPendingIntent // La acción que se ejecuta
            )
            .build()
    }
}