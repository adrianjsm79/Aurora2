package com.tecsup.aurora.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.aurora.data.model.UserProfile
import com.tecsup.aurora.data.repository.AuthRepository
import kotlinx.coroutines.launch
import android.net.Uri
import java.io.File

sealed class ProfileState {
    object Loading : ProfileState()
    data class DataLoaded(val userProfile: UserProfile) : ProfileState()
    object UpdateSuccess : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableLiveData<ProfileState>()
    val state: LiveData<ProfileState> = _state

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

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

    fun onImageSelected(uri: Uri) {
        _selectedImageUri.value = uri
    }

    fun saveProfile(nombre: String, email: String, numero: String, password: String?, imageFile: File?) {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            try {
                val token = authRepository.getToken() ?: throw Exception("No sesión")

                // Llamamos a la nueva función del repositorio
                val newProfile = authRepository.updateProfileComplete(
                    token, nombre, email, numero, password, imageFile
                )

                _state.value = ProfileState.DataLoaded(newProfile)
                _state.value = ProfileState.UpdateSuccess
            } catch (e: Exception) {
                _state.value = ProfileState.Error(e.message ?: "Error")
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