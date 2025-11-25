package com.tecsup.aurora.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tecsup.aurora.data.repository.SettingsRepository

class SecurityViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // LiveData para que la UI sepa si está activado o no
    private val _isFakeShutdownEnabled = MutableLiveData<Boolean>()
    val isFakeShutdownEnabled: LiveData<Boolean> = _isFakeShutdownEnabled

    private val _isSoundListenerEnabled = MutableLiveData<Boolean>()
    val isSoundListenerEnabled: LiveData<Boolean> = _isSoundListenerEnabled

    init {
        // Cargar el estado inicial al crear el ViewModel
        loadState()
    }

    private fun loadState() {
        _isFakeShutdownEnabled.value = settingsRepository.isFakeShutdownEnabled()

        _isSoundListenerEnabled.value = settingsRepository.isSoundListenerEnabled()
    }

    fun toggleFakeShutdown() {
        val currentState = _isFakeShutdownEnabled.value ?: false
        val newState = !currentState

        // Guardar en repositorio
        settingsRepository.setFakeShutdownEnabled(newState)

        // Actualizar LiveData para que la UI reaccione
        _isFakeShutdownEnabled.value = newState
    }

    fun toggleSoundListener(isEnabled: Boolean) {
        settingsRepository.setSoundListenerEnabled(isEnabled)
        _isSoundListenerEnabled.value = isEnabled
    }
}

class SecurityViewModelFactory(
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SecurityViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}