package com.tecsup.aurora.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.aurora.data.repository.SettingsRepository
import com.tecsup.aurora.service.TrackingServiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationViewModel(
    private val settingsRepository: SettingsRepository,
    private val trackingServiceManager: TrackingServiceManager
) : ViewModel() {

    // Observamos directamente el repositorio. Si el servicio cambia esto (ej. se detiene solo),
    // la UI se enterará automáticamente.
    val isTrackingEnabled: LiveData<Boolean> = settingsRepository.isTrackingEnabled

    // Estado de carga para mostrar el ProgressDialog
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Getter para el intervalo actual
    val trackingInterval: Int
        get() = settingsRepository.getTrackingInterval()

    fun onTrackingSwitchChanged(isChecked: Boolean) {
        // Evitamos bucles si el estado ya coincide
        if (isChecked == isTrackingEnabled.value) return

        _isLoading.value = true

        viewModelScope.launch {
            // Simulamos un pequeño delay para dar feedback visual y tiempo al servicio
            delay(800)

            if (isChecked) {
                // Usuario activa -> Iniciar Servicio
                trackingServiceManager.startTracking()
                // Ponemos el estado en true (el servicio lo confirmará, pero esto actualiza la UI rápido)
                settingsRepository.saveTrackingState(true)
            } else {
                // Usuario desactiva -> Parar Servicio
                trackingServiceManager.stopTracking()
                settingsRepository.saveTrackingState(false)
            }

            _isLoading.value = false
        }
    }

    fun onIntervalChanged(intervalSeconds: Int) {
        settingsRepository.saveTrackingInterval(intervalSeconds)

        // Si el rastreo está activo, reiniciamos el servicio para aplicar el nuevo tiempo
        if (isTrackingEnabled.value == true) {
            _isLoading.value = true
            viewModelScope.launch {
                trackingServiceManager.stopTracking()
                delay(500) // Pausa breve para asegurar que se detuvo
                trackingServiceManager.startTracking()
                _isLoading.value = false
            }
        }
    }
}

// Fábrica del ViewModel
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