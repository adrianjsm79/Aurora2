package com.tecsup.aurora.data.repository

import com.tecsup.aurora.data.remote.LocationWebSocketClient
import org.json.JSONObject
import kotlinx.coroutines.flow.SharedFlow

class LocationRepository(
    private val webSocketClient: LocationWebSocketClient
) {

    //Exponemos el flujo de mensajes
    val incomingLocationUpdates: SharedFlow<String> = webSocketClient.incomingMessages

    fun connect(token: String) {
        webSocketClient.connect(token)
    }

    fun sendLocation(
        deviceId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float?
    ) {
        // 1. Crea el objeto JSON que tu LocationConsumer espera
        val json = JSONObject().apply {
            put("type", "location_update")
            put("device_identifier", deviceId)
            put("latitude", latitude)
            put("longitude", longitude)
            put("accuracy", accuracy ?: 0.0)
        }

        // 2. Envía el JSON como un string
        webSocketClient.sendLocation(json.toString())
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }
}