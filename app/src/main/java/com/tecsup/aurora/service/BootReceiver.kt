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

            //repositorio manualmente
            val settingsRepository = SettingsRepository(context)

            //Verifica si el usuario activó la opción
            if (settingsRepository.isStartOnBootEnabled()) {
                Log.d("BootReceiver", "Auto-inicio activado. Iniciando servicio de rastreo...")

                //inicia el servicio usando el Manager existente
                val serviceManager = TrackingServiceManager(context)
                serviceManager.startTracking()

                //Aseguramos que la persistencia diga "true"
                settingsRepository.saveTrackingState(true)
            } else {
                Log.d("BootReceiver", "Auto-inicio desactivado. No se hace nada.")
            }
        }
    }
}