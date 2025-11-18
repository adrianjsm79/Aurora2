package com.tecsup.aurora.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tecsup.aurora.data.repository.SettingsRepository
import com.tecsup.aurora.service.TrackingServiceManager

class LocationViewModel(
    private val settingsRepository: SettingsRepository,
    private val trackingServiceManager: TrackingServiceManager
) : ViewModel() {

    // 1. Expone el estado actual del tracking (leído desde SharedPreferences)
    val isTrackingEnabled: LiveData<Boolean> = settingsRepository.isTrackingEnabled

    // 2. Expone el intervalo actual
    val trackingInterval: Int
        get() = settingsRepository.getTrackingInterval()

    /**
     * Se llama cuando el usuario presiona el Switch principal.
     */
    fun onTrackingSwitchChanged(isEnabled: Boolean) {
        // 1. Guarda la decisión del usuario
        settingsRepository.saveTrackingState(isEnabled)

        // 2. Inicia o detiene el servicio
        if (isEnabled) {
            trackingServiceManager.startTracking()
        } else {
            trackingServiceManager.stopTracking()
        }
    }

    /**
     * Se llama cuando el usuario cambia el RadioButton.
     */
    fun onIntervalChanged(intervalSeconds: Int) {
        settingsRepository.saveTrackingInterval(intervalSeconds)

        // Si el rastreo ya está activo, reinícialo
        // para que tome la nueva configuración
        if (isTrackingEnabled.value == true) {
            trackingServiceManager.stopTracking()
            trackingServiceManager.startTracking()
        }
    }
}


// --- FÁBRICA PARA ESTE VIEWMODEL ---
class LocationViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val trackingServiceManager: TrackingServiceManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocationViewModel(settingsRepository, trackingServiceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}