package com.tecsup.aurora.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.data.model.UserProfile
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.data.repository.DeviceRepository
import com.tecsup.aurora.data.repository.LocationRepository
import com.tecsup.aurora.data.repository.SettingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val userProfile: UserProfile, val devices: List<DeviceResponse>) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _homeState = MutableLiveData<HomeState>(HomeState.Loading)
    val homeState: LiveData<HomeState> = _homeState

    //Guardamos la lista actual en memoria para poder modificarla rápido
    private var currentDevices: List<DeviceResponse> = emptyList()
    private var currentUserProfile: UserProfile? = null

    //Señal de actualización de tiempo
    private val _timeUpdateEvent = MutableLiveData<Unit>()
    val timeUpdateEvent: LiveData<Unit> = _timeUpdateEvent


    init {
        Log.d("AURORA_DEBUG", "HomeViewModel: Inicializando...")
        loadData()
        //escucha de actualizaciones en tiempo real
        observeRealtimeUpdates()
        startTimeTicker()
    }

    fun loadData() {
        viewModelScope.launch {
            _homeState.value = HomeState.Loading
            try {
                val token = authRepository.getToken() ?: return@launch

                // 1. Conectamos el WebSocket para empezar a escuchar
                locationRepository.connect(token)

                val profileDeferred = async { authRepository.getUserProfile(token) }
                val devicesDeferred = async { deviceRepository.getDevices(token) }

                currentUserProfile = profileDeferred.await()
                currentDevices = devicesDeferred.await()

                _homeState.value = HomeState.Success(currentUserProfile!!, currentDevices)

            } catch (e: Exception) {
                _homeState.value = HomeState.Error(e.message ?: "Error")
            }
        }
    }

    private fun observeRealtimeUpdates() {
        viewModelScope.launch {
            // Escuchamos el flujo infinito de mensajes del WebSocket
            locationRepository.incomingLocationUpdates.collect { jsonString ->
                try {
                    // 1. Parseamos el mensaje
                    val json = JSONObject(jsonString)
                    val type = json.optString("type")

                    // Solo nos interesan las actualizaciones de ubicación
                    if (type == "location_update") {
                        val deviceId = json.getInt("device_id")
                        val lat = json.getDouble("latitude")
                        val lon = json.getDouble("longitude")

                        // 2. Actualizamos la lista localmente
                        updateDeviceInList(deviceId, lat, lon)

                        val command = json.optString("command")
                        if (command == "update_status") {
                            val isLost = json.optBoolean("is_lost")
                            
                            Log.w("AURORA_SECURITY", "Estado de robo actualizado: $isLost")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error procesando update: $e")
                }
            }
        }
    }

    private fun updateDeviceInList(deviceId: Int, lat: Double, lon: Double) {
        // Creamos una NUEVA lista (copia) modificando solo el dispositivo que cambió
        val updatedList = currentDevices.map { device ->
            if (device.id == deviceId) {
                // si encontramos dispositivo Lo actualizamos.
                device.copy(
                    latitude = lat,
                    longitude = lon,
                    // Actualizamos la fecha a "Ahora mismo" (formato ISO)
                    last_seen = Instant.now().toString()
                )
            } else {
                device
            }
        }

        // Guardamos la referencia y actualizamos la UI
        currentDevices = updatedList
        if (currentUserProfile != null) {
            // Esto dispara el observer en la Activity y refresca la lista visualmente
            _homeState.postValue(HomeState.Success(currentUserProfile!!, updatedList))
        }
    }

    private fun startTimeTicker() {
        viewModelScope.launch {
            while (isActive) { // Mientras el ViewModel esté vivo
                delay(30_000) // Espera 30 segundos
                _timeUpdateEvent.postValue(Unit) // ¡Ding! Hora de actualizar
            }
        }
    }

    fun toggleDeviceLostState(device: DeviceResponse) {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: return@launch

                // Invertimos el estado actual
                val newState = !device.is_lost

                // 1. Llamada a la API
                deviceRepository.updateDeviceStatus(token, device.id, newState)

                // 2. Feedback visual inmediato (opcional, o esperar al websocket)
                // Por ahora, recargamos la lista completa para asegurar consistencia
                loadData()

            } catch (e: Exception) {
                _homeState.value = HomeState.Error("Error al actualizar: ${e.message}")
            }
        }
    }

    fun deleteDevice(deviceId: Int) {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: return@launch
                deviceRepository.deleteDevice(token, deviceId)
                loadData() // Recargar lista
            } catch (e: Exception) {
                _homeState.value = HomeState.Error("Error al eliminar")
            }
        }
    }

    fun renameDevice(deviceId: Int, newName: String) {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: return@launch

                // Llamada al repositorio (que usa PATCH)
                deviceRepository.renameDevice(token, deviceId, newName)

                // Recargar la lista para reflejar el nuevo nombre en la UI
                loadData()

            } catch (e: Exception) {
                _homeState.value = HomeState.Error("Error al cambiar nombre: ${e.message}")
            }
        }
    }
}

class HomeViewModelFactory(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(authRepository, deviceRepository, locationRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
