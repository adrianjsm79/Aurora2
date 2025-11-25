package com.tecsup.aurora.utils

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.delay

class MorseEmitter {

    // Diccionario Morse simplificado
    private val morseCode = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '0' to "-----"
    )

    // Usa STREAM_ALARM para que suene fuerte incluso si multimedia está bajo
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)

    suspend fun sendSignal(secretWord: String) {
        val pattern = secretWord.uppercase().map { char ->
            morseCode[char] ?: ""
        }.joinToString(" ") // Espacio entre letras

        playPattern(pattern)
    }

    private suspend fun playPattern(pattern: String) {
        for (char in pattern) {
            when (char) {
                '.' -> {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 150) // Punto: Agudo
                    delay(200)
                }
                '-' -> {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 400) // Guion: Grave y largo
                    delay(450)
                }
                ' ' -> {
                    delay(300) // Silencio entre letras
                }
            }
        }
        // Señal de fin
        toneGenerator.stopTone()
    }
}