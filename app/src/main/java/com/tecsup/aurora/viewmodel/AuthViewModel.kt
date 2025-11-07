package com.tecsup.aurora.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aurora.data.model.RegisterRequest
import com.tecsup.aurora.data.repository.AuthRepository
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
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

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

        // 3. Corrutina para llamar al repositorio
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                repository.login(email, pass)
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Error desconocido")
            }
        }
    }

}

// (Necesitarás un ViewModelFactory para pasar el 'repository'
// al constructor del ViewModel)