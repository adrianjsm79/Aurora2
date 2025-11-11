package com.tecsup.aurora.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tecsup.aurora.data.model.RegisterRequest
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.ui.activities.LoginActivity
import com.tecsup.aurora.utils.DeviceHelper
import kotlinx.coroutines.launch

// Estado para la UI: Cargando, Éxito o Error
sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    object Success : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

// El ViewModel necesita el Repositorio para trabajar
class AuthViewModel(
    private val repository: AuthRepository,
    application: Application
) : AndroidViewModel(application) { // <-- Llama al constructor padre

    // LiveData privado que solo el ViewModel puede modificar
    private val _registrationState = MutableLiveData<RegistrationState>(RegistrationState.Idle)

    // LiveData público que la Activity "observa"
    val registrationState: LiveData<RegistrationState> = _registrationState


    fun onRegisterClicked(
        nombre: String,
        email: String,
        numero: String,
        pass1: String,
        pass2: String
    ) {
        // --- 1. Lógica de Validación (El VM es responsable) ---
        if (nombre.isBlank() || email.isBlank() || numero.isBlank() || pass1.isBlank()) {
            _registrationState.value = RegistrationState.Error("Todos los campos son obligatorios")
            return
        }
        if (pass1 != pass2) {
            _registrationState.value = RegistrationState.Error("Las contraseñas no coinciden")
            return
        }

        // --- 2. Iniciar la Corrutina ---
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                // --- 3. Crear Petición y Llamar al Repositorio ---
                val request = RegisterRequest(email, nombre, numero, pass1, pass2)
                repository.registerUser(request)

                // --- 4. Éxito ---
                _registrationState.value = RegistrationState.Success

            } catch (e: Exception) {
                // --- 5. Error ---
                // (En producción, deberías manejar errores de red específicos)
                _registrationState.value = RegistrationState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    // LÓGICA DE LOGIN ---

    // 1. LiveData para el estado de Login
    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    // 2. Función que la Activity llamará
    fun onLoginClicked(email: String, pass: String) {
        // Validación simple
        if (email.isBlank() || pass.isBlank()) {
            _loginState.value = LoginState.Error("Email y contraseña son obligatorios")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // Paso 1: Autenticar y obtener token
                val token = repository.login(email, pass)
                val context = getApplication<Application>().applicationContext
                // Paso 2: Obtener datos del dispositivo
                val deviceId = DeviceHelper.getDeviceIdentifier(context.applicationContext)
                val deviceName = DeviceHelper.getDeviceName()

                // Paso 3: Registrar el dispositivo en el backend
                repository.registerDevice(token, deviceName, deviceId)

                // ¡Éxito en ambos pasos!
                _loginState.value = LoginState.Success

            } catch (e: Exception) {
                // Si falla el login O el registro del dispositivo, se reporta error
                _loginState.value = LoginState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    // Dentro de tu clase AuthViewModel.kt
    fun onLogoutClicked() {
        viewModelScope.launch {
            try {
                repository.logout() // Llama al método del repositorio inyectado
                // Opcional: Podrías emitir un estado de LogoutSuccess si necesitas que la UI reaccione
                // _logoutState.value = LogoutState.Success
            } catch (e: Exception) {
                // Manejar cualquier error que pueda ocurrir al limpiar el token
                // _logoutState.value = LogoutState.Error(e.message)
            }
        }
    }


}