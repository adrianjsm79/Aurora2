package com.tecsup.aurora.data.repository

import com.tecsup.aurora.data.remote.ApiService
import com.tecsup.aurora.data.model.DeviceResponse

// Este repositorio gestionará la lógica de obtener dispositivos
class DeviceRepository(private val apiService: ApiService) {

    suspend fun getDevices(token: String): List<DeviceResponse> {
        val authToken = "Bearer $token"
        val response = apiService.getDevices(authToken)
        if (!response.isSuccessful) {
            throw Exception("Error al cargar dispositivos: ${response.code()}")
        }
        return response.body() ?: emptyList()
    }

    suspend fun updateDeviceStatus(token: String, deviceId: Int, isLost: Boolean) {
        val authToken = "Bearer $token"
        // Creamos un mapa con solo el campo que queremos cambiar
        val updates = mapOf("is_lost" to isLost)

        val response = apiService.updateDevice(authToken, deviceId, updates)
        if (!response.isSuccessful) {
            throw Exception("Error al actualizar estado: ${response.code()}")
        }
    }

    suspend fun deleteDevice(token: String, deviceId: Int) {
        val authToken = "Bearer $token"
        val response = apiService.deleteDevice(authToken, deviceId)
        if (!response.isSuccessful) {
            throw Exception("Error al eliminar dispositivo")
        }
    }

    suspend fun renameDevice(token: String, deviceId: Int, newName: String) {
        val authToken = "Bearer $token"
        val updates = mapOf("name" to newName)

        val response = apiService.updateDevice(authToken, deviceId, updates)
        if (!response.isSuccessful) {
            throw Exception("Error al renombrar: ${response.code()}")
        }
    }
}