package com.tecsup.aurora.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.data.model.UserProfile
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.data.repository.DeviceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

// Estado para la UI del Home
sealed class HomeState {
    object Loading : HomeState()
    data class Success(
        val userProfile: UserProfile,
        val devices: List<DeviceResponse>
    ) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _homeState = MutableLiveData<HomeState>(HomeState.Loading)
    val homeState: LiveData<HomeState> = _homeState

    // 1. Inicia la carga de datos cuando se crea el ViewModel
    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _homeState.value = HomeState.Loading
            try {
                // Obtenemos el token de Realm
                val token = authRepository.getToken()
                if (token == null) {
                    _homeState.value = HomeState.Error("Sesión no encontrada")
                    return@launch
                }

                // 2. Buscamos el perfil Y los dispositivos en paralelo (más rápido)
                // (Necesitarás 'kotlinx-coroutines-android' para 'async')
                val profileDeferred = viewModelScope.async { authRepository.getUserProfile(token) }
                val devicesDeferred = viewModelScope.async { deviceRepository.getDevices(token) }

                val profile = profileDeferred.await()
                val devices = devicesDeferred.await()

                // 3. Enviamos el estado de Éxito
                _homeState.value = HomeState.Success(profile, devices)

            } catch (e: Exception) {
                _homeState.value = HomeState.Error(e.message ?: "Error al cargar datos")
            }
        }
    }
}

// 4. Fábrica para el HomeViewModel
class HomeViewModelFactory(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(authRepository, deviceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}