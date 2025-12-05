package com.tecsup.aurora.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.data.repository.DeviceRepository
import com.tecsup.aurora.data.repository.LocationRepository
import com.tecsup.aurora.data.repository.MapRepository
import com.tecsup.aurora.data.repository.SettingsRepository
import com.tecsup.aurora.service.TrackingServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject

// --- CLASES DE ESTADO DE UI ---

data class MapOverlays(
    val routeTargetId: Int? = null,
    val routePoints: List<LatLng> = emptyList(), // Puntos de la línea de ruta (Calles reales)
    val traces: Map<Int, List<LatLng>> = emptyMap() // Mapa de DeviceID -> Lista de puntos ("migas")
)

data class MapUiState(
    val devices: List<DeviceResponse> = emptyList(),
    val currentUserId: Int = -1
)

class MapViewModel(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val trackingServiceManager: TrackingServiceManager,
    private val mapRepository: MapRepository
) : ViewModel() {

    // REEMPLAZA CON TU API KEY REAL DE GOOGLE MAPS (La que habilitaste para Directions API)
    private val GOOGLE_API_KEY = "TU_API_KEY_AQUI"

    // --- LiveData ---
    private val _uiState = MutableLiveData<MapUiState>()
    val uiState: LiveData<MapUiState> = _uiState

    private val _mapOverlays = MutableLiveData<MapOverlays>()
    val mapOverlays: LiveData<MapOverlays> = _mapOverlays

    // Observamos el estado del rastreo local directamente del repo
    val isTrackingActive: LiveData<Boolean> = settingsRepository.isTrackingEnabled

    init {
        loadData()
        observeRealtimeUpdates()
        // Cargar rutas/rastros guardados de sesiones anteriores
        refreshOverlays()
    }

    // --- CARGA DE DATOS INICIAL ---

    fun loadData() {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: return@launch

                // Carga paralela para velocidad
                val profileDeferred = async { authRepository.getUserProfile(token) }
                val devicesDeferred = async { deviceRepository.getDevices(token) }

                val profile = profileDeferred.await()
                val devices = devicesDeferred.await()

                Log.d("MAP_DEBUG", "Mi ID de Usuario: ${profile.id}")

                _uiState.value = MapUiState(devices, profile.id)

                // Conectar al WebSocket
                locationRepository.connect(token)

                // Refrescar visualización de líneas
                refreshOverlays()

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error cargando datos iniciales", e)
            }
        }
    }

    // --- LÓGICA DE BOTÓN DE UBICACIÓN ---

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

    // --- WEBSOCKET / TIEMPO REAL ---

    private fun observeRealtimeUpdates() {
        viewModelScope.launch {
            locationRepository.incomingLocationUpdates.collect { jsonString ->
                try {
                    val json = JSONObject(jsonString)
                    val type = json.optString("type")

                    // A. Actualización de GPS
                    if (type == "location_update" && json.has("latitude")) {
                        val deviceId = json.getInt("device_id")
                        val lat = json.getDouble("latitude")
                        val lon = json.getDouble("longitude")
                        val acc = json.optDouble("accuracy", 0.0).toFloat()

                        updateDeviceLocation(deviceId, lat, lon, acc)
                    }

                    // B. Actualización de Estado (Perdido/Encontrado)
                    if (json.has("command") && json.getString("command") == "update_status") {
                        val deviceId = json.getInt("device_id")
                        val isLost = json.getBoolean("is_lost")
                        updateDeviceStatusLocal(deviceId, isLost)
                    }
                } catch (e: Exception) {
                    Log.e("MapViewModel", "Error procesando mensaje WS", e)
                }
            }
        }
    }

    private fun updateDeviceLocation(deviceId: Int, lat: Double, lon: Double, acc: Float) {
        // 1. Actualizar lista en memoria (Mueve el marcador visualmente)
        val current = _uiState.value ?: return
        val updatedList = current.devices.map {
            if (it.id == deviceId) it.copy(latitude = lat, longitude = lon, accuracy = acc) else it
        }
        _uiState.postValue(current.copy(devices = updatedList))

        // 2. Guardar "miga de pan" en Realm si el rastreo está activo para este dispositivo
        mapRepository.addTracePoint(deviceId, lat, lon)

        // 3. Recalcular líneas (ruta y rastro) con la nueva posición
        refreshOverlays()
    }

    private fun updateDeviceStatusLocal(deviceId: Int, isLost: Boolean) {
        val current = _uiState.value ?: return
        val updatedList = current.devices.map {
            if (it.id == deviceId) it.copy(is_lost = isLost) else it
        }
        _uiState.postValue(current.copy(devices = updatedList))
    }

    // --- GESTIÓN DE LÍNEAS Y RUTAS (OVERLAYS) ---

    fun refreshOverlays() {
        viewModelScope.launch(Dispatchers.IO) {
            val activeRouteId = mapRepository.getActiveRouteDeviceId()
            val tracesMap = mutableMapOf<Int, List<LatLng>>()

            // 1. Recuperar Rastros ("Migas") de Realm
            _uiState.value?.devices?.forEach { device ->
                val points = mapRepository.getTracePoints(device.id)
                if (points.isNotEmpty()) {
                    tracesMap[device.id] = points.map { LatLng(it.latitude, it.longitude) }
                }
            }

            // 2. Calcular Ruta de Navegación (Google Directions)
            var routePoints: List<LatLng> = emptyList()

            if (activeRouteId != null) {
                val myId = _uiState.value?.currentUserId
                // Buscamos MI dispositivo actual (asumimos que es el que tiene mi ID de usuario y ubicación válida)
                val myDevice = _uiState.value?.devices?.find { it.user == myId && it.latitude != null }
                val targetDevice = _uiState.value?.devices?.find { it.id == activeRouteId }

                if (myDevice?.latitude != null && targetDevice?.latitude != null) {
                    val origin = LatLng(myDevice.latitude!!, myDevice.longitude!!)
                    val dest = LatLng(targetDevice.latitude!!, targetDevice.longitude!!)

                    // Llamada a la API de Google para ruta real por calles
                    routePoints = mapRepository.getRealRoute(origin, dest, GOOGLE_API_KEY)

                    // Fallback a línea recta si falla la API
                    if (routePoints.isEmpty()) {
                        routePoints = listOf(origin, dest)
                    }
                }
            }

            _mapOverlays.postValue(MapOverlays(activeRouteId, routePoints, tracesMap))
        }
    }

    // --- ACCIONES DEL MENÚ CONTEXTUAL ---

    fun setRouteTarget(deviceId: Int) {
        mapRepository.setRouting(deviceId)
        refreshOverlays() // Dispara el recálculo de la ruta
    }

    fun toggleTrace(deviceId: Int) {
        val isEnabled = mapRepository.toggleTracing(deviceId)
        if (isEnabled) {
            // Si activamos, guardamos el punto actual como inicio
            val device = _uiState.value?.devices?.find { it.id == deviceId }
            if (device?.latitude != null) {
                mapRepository.addTracePoint(deviceId, device.latitude!!, device.longitude!!)
            }
        }
        refreshOverlays()
    }

    fun clearTrace(deviceId: Int) {
        mapRepository.clearTrace(deviceId)
        refreshOverlays()
    }

    fun markAsLost(deviceId: Int, isLost: Boolean) {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: return@launch
                // Actualizar en Backend (Dispara WebSocket de alerta)
                deviceRepository.updateDeviceStatus(token, deviceId, isLost)
                // Actualizar Local (Feedback instantáneo)
                updateDeviceStatusLocal(deviceId, isLost)
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error al marcar como perdido", e)
            }
        }
    }
}

// --- FÁBRICA ---

class MapViewModelFactory(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val trackingServiceManager: TrackingServiceManager,
    private val mapRepository: MapRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(
                authRepository,
                deviceRepository,
                locationRepository,
                settingsRepository,
                trackingServiceManager,
                mapRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}