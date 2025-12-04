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
import com.tecsup.aurora.data.repository.SettingsRepository
import com.tecsup.aurora.service.TrackingServiceManager
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject

class MapViewModel(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val trackingServiceManager: TrackingServiceManager
) : ViewModel() {

    data class MapUiState(
        val devices: List<DeviceResponse> = emptyList(),
        val currentUserId: Int = -1
    )

    private val _uiState = MutableLiveData<MapUiState>()
    val uiState: LiveData<MapUiState> = _uiState

    // Observamos el estado del rastreo directamente del repo
    val isTrackingActive: LiveData<Boolean> = settingsRepository.isTrackingEnabled

    init {
        loadData()
        observeRealtimeUpdates()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: return@launch
                val profileDeferred = async { authRepository.getUserProfile(token) }
                val devicesDeferred = async { deviceRepository.getDevices(token) }

                val profile = profileDeferred.await()
                val devices = devicesDeferred.await()

                Log.d("MAP_DEBUG", "Mi ID de Usuario: ${profile.id}")

                _uiState.value = MapUiState(devices, profile.id)
                locationRepository.connect(token)

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error", e)
            }
        }
    }

    fun toggleTracking() {
        val isEnabled = isTrackingActive.value == true
        if (isEnabled) {
            trackingServiceManager.stopTracking()
            settingsRepository.saveTrackingState(false)
        } else {
            trackingServiceManager.startTracking()
            settingsRepository.saveTrackingState(true)
        }
    }

    private fun observeRealtimeUpdates() {
        viewModelScope.launch {
            locationRepository.incomingLocationUpdates.collect { jsonString ->
                try {
                    val json = JSONObject(jsonString)
                    if (json.optString("type") == "location_update" && json.has("latitude")) {
                        val deviceId = json.getInt("device_id")
                        val lat = json.getDouble("latitude")
                        val lon = json.getDouble("longitude")
                        val acc = json.optDouble("accuracy", 0.0).toFloat()

                        updateDeviceLocation(deviceId, lat, lon, acc)
                    }
                } catch (e: Exception) { }
            }
        }
    }

    private fun updateDeviceLocation(deviceId: Int, lat: Double, lon: Double, acc: Float) {
        val current = _uiState.value ?: return
        val updatedList = current.devices.map {
            if (it.id == deviceId) it.copy(latitude = lat, longitude = lon, accuracy = acc) else it
        }
        _uiState.postValue(current.copy(devices = updatedList))
    }
}

class MapViewModelFactory(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val trackingServiceManager: TrackingServiceManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(authRepository, deviceRepository, locationRepository, settingsRepository, trackingServiceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}