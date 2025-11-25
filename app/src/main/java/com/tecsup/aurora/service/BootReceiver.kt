package com.tecsup.aurora.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tecsup.aurora.data.repository.SettingsRepository

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Sistema reiniciado. Verificando configuración...")

            // 1. Creamos el repositorio manualmente (no hay inyección aquí)
            val settingsRepository = SettingsRepository(context)

            // 2. Verificamos si el usuario activó la opción
            if (settingsRepository.isStartOnBootEnabled()) {
                Log.d("BootReceiver", "Auto-inicio activado. Iniciando servicio de rastreo...")

                // 3. Iniciamos el servicio usando tu Manager existente
                val serviceManager = TrackingServiceManager(context)
                serviceManager.startTracking()

                // (Opcional) Aseguramos que la persistencia diga "true"
                settingsRepository.saveTrackingState(true)
            } else {
                Log.d("BootReceiver", "Auto-inicio desactivado. No se hace nada.")
            }
        }
    }
}