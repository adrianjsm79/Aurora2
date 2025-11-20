package com.tecsup.aurora.data.remote

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class LocationWebSocketClient {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    //tubo de datos para mensajes entrantes
    private val _incomingMessages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "Conectado al servidor")
        }

        //Cuando llega un mensaje del Backend (Redis -> Django -> App)
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket", "Mensaje recibido: $text")
            // Lo "emitimos" para que el viewmodel lo reciba
            _incomingMessages.tryEmit(text)
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
        webSocket?.close(1000, "Cerrando")
    }
}