package com.tecsup.aurora.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.aurora.data.model.UserProfile
import com.tecsup.aurora.data.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Loading : ProfileState()
    data class DataLoaded(val userProfile: UserProfile) : ProfileState()
    object UpdateSuccess : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableLiveData<ProfileState>()
    val state: LiveData<ProfileState> = _state

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            try {
                val token = authRepository.getToken() ?: throw Exception("No hay sesión")
                val profile = authRepository.getUserProfile(token)
                _state.value = ProfileState.DataLoaded(profile)
            } catch (e: Exception) {
                _state.value = ProfileState.Error(e.message ?: "Error de carga")
            }
        }
    }

    fun saveProfile(nombre: String, numero: String) {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            try {
                val token = authRepository.getToken() ?: throw Exception("No hay sesión")
                authRepository.updateProfile(token, nombre, numero)
                _state.value = ProfileState.UpdateSuccess
            } catch (e: Exception) {
                _state.value = ProfileState.Error(e.message ?: "Error al guardar")
            }
        }
    }
}

class ProfileViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}