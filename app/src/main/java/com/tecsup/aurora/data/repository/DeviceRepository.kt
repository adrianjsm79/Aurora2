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
}