package com.tecsup.aurora.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.data.repository.DeviceRepository
import com.tecsup.aurora.data.repository.LocationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject

class MapViewModel(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    // Estado de la UI: Lista de dispositivos + ID del usuario actual (para diferenciar colores)
    data class MapUiState(
        val devices: List<DeviceResponse> = emptyList(),
        val currentUserId: Int = -1
    )

    private val _uiState = MutableLiveData<MapUiState>()
    val uiState: LiveData<MapUiState> = _uiState

    init {
        loadData()
        observeRealtimeUpdates()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: return@launch

                // 1. Obtenemos el perfil para saber "quién soy yo" (mi ID)
                val profileDeferred = async { authRepository.getUserProfile(token) }
                // 2. Obtenemos todos los dispositivos (míos y de contactos)
                val devicesDeferred = async { deviceRepository.getDevices(token) }

                val profile = profileDeferred.await()
                val devices = devicesDeferred.await()

                _uiState.value = MapUiState(devices, profile.id)

                // 3. Conectar WebSocket para recibir movimientos en vivo
                locationRepository.connect(token)

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error cargando datos del mapa", e)
            }
        }
    }

    private fun observeRealtimeUpdates() {
        viewModelScope.launch {
            locationRepository.incomingLocationUpdates.collect { jsonString ->
                try {
                    val json = JSONObject(jsonString)
                    val type = json.optString("type")

                    // Si llega una actualización de ubicación
                    if (type == "location_update" && json.has("latitude")) {
                        val deviceId = json.getInt("device_id")
                        val lat = json.getDouble("latitude")
                        val lon = json.getDouble("longitude")

                        // Actualizamos la lista localmente para mover el marcador
                        updateDeviceLocation(deviceId, lat, lon)
                    }
                } catch (e: Exception) {
                    Log.e("MapViewModel", "Error procesando update", e)
                }
            }
        }
    }

    private fun updateDeviceLocation(deviceId: Int, lat: Double, lon: Double) {
        val currentState = _uiState.value ?: return
        val updatedList = currentState.devices.map { device ->
            if (device.id == deviceId) {
                device.copy(latitude = lat, longitude = lon)
            } else {
                device
            }
        }
        _uiState.postValue(currentState.copy(devices = updatedList))
    }
}

// Fábrica para crear el MapViewModel
class MapViewModelFactory(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(authRepository, deviceRepository, locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}