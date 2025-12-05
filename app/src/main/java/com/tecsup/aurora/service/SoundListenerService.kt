package com.tecsup.aurora.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.*
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tecsup.aurora.R
import kotlinx.coroutines.*
import kotlin.math.abs

class SoundListenerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isListening = false
    private var audioRecord: AudioRecord? = null

    // La clave que esperamos (ej. "FLORA" en morse: ..-. .-.. --- .-.)
    private val secretPattern = "..-. .-.. --- .-."

    // Buffer para acumular lo que escuchamos ("." o "-")
    private val listenedPattern = StringBuilder()
    private var lastSoundTime = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        startListening()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "sound_guard_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Aurora Sound Guard", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Escucha Activa")
            .setContentText("Aurora está escuchando señales de emergencia.")
            .setSmallIcon(R.drawable.ic_security) // Asegúrate de tener este icono
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(200, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(200, notification)
        }
    }

    private fun startListening() {
        if (isListening) return
        isListening = true

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            //El Permiso CHECK debe hacerse en la Activity antes de lanzar el servicio
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            audioRecord?.startRecording()

            serviceScope.launch {
                val buffer = ShortArray(bufferSize)

                while (isListening) {
                    val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (readResult > 0) {
                        analyzeAudio(buffer, readResult)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SoundService", "Error al iniciar micrófono", e)
            stopSelf()
        }
    }

    private fun analyzeAudio(buffer: ShortArray, readSize: Int) {
        // Calcular volumen promedio (RMS)
        var sum = 0.0
        for (i in 0 until readSize) {
            sum += abs(buffer[i].toInt())
        }
        val amplitude = sum / readSize

        // Umbral de "Sonido Fuerte"
        val threshold = 200

        val currentTime = System.currentTimeMillis()

        if (amplitude > threshold) {
            // Estamos escuchando un sonido
            val duration = currentTime - lastSoundTime

            // Si el silencio fue muy largo, reseteamos el patrón
            if (duration > 2000) {
                listenedPattern.clear()
            }
            lastSoundTime = currentTime

        }

        if (amplitude > 1000) { // Umbral de "grito"
            triggerAlarm()
        }
    }

    private fun triggerAlarm() {
        if (!isListening) return // Evitar disparos múltiples
        isListening = false // Dejar de escuchar para sonar

        // 1. Subir volumen al máximo
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        // 2. Tocar sonido de alarma
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        serviceScope.launch {
            repeat(10) {
                tone.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
                delay(1500)
            }
            tone.release()
            // Reiniciar escucha
            startListening()
        }
    }

    override fun onDestroy() {
        isListening = false
        audioRecord?.stop()
        audioRecord?.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}