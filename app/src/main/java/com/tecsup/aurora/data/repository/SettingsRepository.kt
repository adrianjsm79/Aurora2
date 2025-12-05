package com.tecsup.aurora.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "AuroraSettings"
        private const val KEY_TRACKING_ENABLED = "isTrackingEnabled"
        private const val KEY_TRACKING_INTERVAL = "trackingInterval"
        private const val KEY_START_ON_BOOT = "startOnBoot"

        private const val KEY_DEVICE_IS_LOST = "deviceIsLost"
        private const val KEY_FAKE_SHUTDOWN_MANUAL = "fakeShutdownManual"

        private const val KEY_SOUND_LISTENER = "soundListenerEnabled"
    }

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // LiveData para que la UI reaccionen a cambios
    private val _isTrackingEnabled = MutableLiveData<Boolean>(getTrackingState())
    val isTrackingEnabled: LiveData<Boolean> = _isTrackingEnabled

    //ESTADO DEL TRACKING

    fun saveTrackingState(isEnabled: Boolean) {
        sharedPrefs.edit {
            putBoolean(KEY_TRACKING_ENABLED, isEnabled)
        }
        // PostValue es seguro para llamar desde hilos de fondo (como el Servicio)
        _isTrackingEnabled.postValue(isEnabled)
    }

    fun getTrackingState(): Boolean {
        return sharedPrefs.getBoolean(KEY_TRACKING_ENABLED, false)
    }

    //INTERVALO DE TIEMPO

    fun saveTrackingInterval(intervalSeconds: Int) {
        sharedPrefs.edit {
            putInt(KEY_TRACKING_INTERVAL, intervalSeconds)
        }
    }

    fun getTrackingInterval(): Int {
        return sharedPrefs.getInt(KEY_TRACKING_INTERVAL, 10)
    }

    fun saveStartOnBoot(isEnabled: Boolean) {
        sharedPrefs.edit {
            putBoolean(KEY_START_ON_BOOT, isEnabled)
        }
    }

    fun isStartOnBootEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_START_ON_BOOT, false)
    }

    // ESTADO DE ROBO

    fun setDeviceLost(isLost: Boolean) {
        sharedPrefs.edit { putBoolean(KEY_DEVICE_IS_LOST, isLost) }
    }

    fun isDeviceLost(): Boolean {
        return sharedPrefs.getBoolean(KEY_DEVICE_IS_LOST, false)
    }

    //ACTIVACIÓN MANUAL (Por el usuario en SecurityActivity)

    fun setFakeShutdownEnabled(isEnabled: Boolean) {
        sharedPrefs.edit { putBoolean(KEY_FAKE_SHUTDOWN_MANUAL, isEnabled) }
    }

    fun isFakeShutdownEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_FAKE_SHUTDOWN_MANUAL, false)
    }

    //BÚSQUEDA POR SONIDO
    fun setSoundListenerEnabled(isEnabled: Boolean) {
        sharedPrefs.edit { putBoolean(KEY_SOUND_LISTENER, isEnabled) }
    }

    fun isSoundListenerEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_SOUND_LISTENER, false)
    }
}