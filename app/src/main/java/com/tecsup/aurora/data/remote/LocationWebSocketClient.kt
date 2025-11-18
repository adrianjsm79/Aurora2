package com.tecsup.aurora.data.remote

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class LocationWebSocketClient {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "Conectado al servidor")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("WebSocket", "Error de conexión: ${t.message}", t)
            // Aquí podrías intentar reconectar
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "Cerrando conexión")
            webSocket.close(1000, null)
        }
    }

    fun connect(token: String) {

        val wsUrl = "wss://aurorabackend.up.railway.app/ws/location/?token=$token"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, listener)
    }

    fun sendLocation(jsonMessage: String) {
        val sent = webSocket?.send(jsonMessage)
        if (sent != true) {
            Log.w("WebSocket", "No se pudo enviar el mensaje, WebSocket no conectado.")
            // Podrías re-intentar conectar aquí
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Servicio detenido")
    }
}