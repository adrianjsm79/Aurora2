package com.tecsup.aurora.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Gestiona la persistencia de las configuraciones del usuario.
 * Usa SharedPreferences para guardar valores simples.
 */
class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "AuroraSettings"
        private const val KEY_TRACKING_ENABLED = "isTrackingEnabled"
        private const val KEY_TRACKING_INTERVAL = "trackingInterval"
    }

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- LiveData para observar cambios en tiempo real ---

    // LiveData privado
    private val _isTrackingEnabled = MutableLiveData<Boolean>(isTrackingEnabled())
    // LiveData público (solo lectura)
    val isTrackingEnabled: LiveData<Boolean> = _isTrackingEnabled


    // --- Funciones de guardado y lectura ---

    fun saveTrackingState(isEnabled: Boolean) {
        sharedPrefs.edit {
            putBoolean(KEY_TRACKING_ENABLED, isEnabled)
        }
        _isTrackingEnabled.postValue(isEnabled) // Notifica a los observadores
    }

    fun isTrackingEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_TRACKING_ENABLED, false) // Default: false
    }

    fun saveTrackingInterval(intervalSeconds: Int) {
        sharedPrefs.edit {
            putInt(KEY_TRACKING_INTERVAL, intervalSeconds)
        }
    }

    fun getTrackingInterval(): Int {
        return sharedPrefs.getInt(KEY_TRACKING_INTERVAL, 10) // Default: 10 segundos
    }
}