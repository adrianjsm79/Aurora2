package com.tecsup.aurora.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceHelper {

    private val PROBLEMATIC_MANUFACTURERS = listOf(
        "xiaomi",
        "oppo",
        "vivo",
        "huawei",
        "honor",
        "meizu",
        "oneplus"
    )

    /**
     * Checks if the device is from a manufacturer known for aggressive
     * battery optimization that can kill background services.
     */
    fun isProblematicManufacturer(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return PROBLEMATIC_MANUFACTURERS.contains(manufacturer)
    }

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
        if (model.startsWith(manufacturer, ignoreCase = true)) {
            return model.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        return "${manufacturer.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} $model"
    }
}
