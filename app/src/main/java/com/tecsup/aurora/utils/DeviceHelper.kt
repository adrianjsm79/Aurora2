package com.tecsup.aurora.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.annotation.SuppressLint

object DeviceHelper {

    /**
     * Obtiene el ID único del dispositivo.
     * Es estable para este dispositivo físico.
     */
    @SuppressLint("HardwareIds")
    fun getDeviceIdentifier(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    /**
     * Obtiene un nombre amigable para el dispositivo (ej. "Pixel 8")
     */
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        if (model.startsWith(manufacturer)) {
            return model.capitalize()
        }
        return "${manufacturer.capitalize()} $model"
    }
}