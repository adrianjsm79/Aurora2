package com.tecsup.aurora.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.tecsup.aurora.R
import com.tecsup.aurora.service.TrackingService
import com.tecsup.aurora.ui.activities.LocationActivity

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
                lockscreenVisibility = Notification.VISIBILITY_SECRET //oculta en la pantalla de bloqueo
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun createNotification(context: Context): Notification {

        val openAppIntent = Intent(context, LocationActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        //acción de parar
        val stopServiceIntent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP_SERVICE
        }

        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopServiceIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        //configuracion de remoteviews para el layout de la notify
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_tracking)

        // Asignar la acción al botón del layout XML
        // "Cuando toquen R.id.btn_stop_tracking, ejecuta stopPendingIntent"
        remoteViews.setOnClickPendingIntent(R.id.stop, stopPendingIntent)

        // Cambiar texto dinámicamente (opcional)
        //remoteViews.setTextViewText(R.id.notification_status, "texto ejemplo")

        // Construye la notificación
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_aurora) // Icono obligatorio para la barra de estado (pequeño)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()) // Estilo esquinas redondeadas
            .setCustomContentView(remoteViews) // insertamos el layout
            .setContentIntent(openPendingIntent) // Al tocar la notificación, abre la app
            .setOngoing(true) // No se puede deslizar para borrar
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Oculto en pantalla de bloqueo
            .build()
    }
}
