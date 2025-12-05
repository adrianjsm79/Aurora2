package com.tecsup.aurora.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aurora.data.model.RegisterRequest
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.utils.DeviceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- ESTADOS DE LA UI (Se definen una sola vez aquí) ---

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

sealed class SessionState {
    object Loading : SessionState()
    object Authenticated : SessionState()
    object Unauthenticated : SessionState()
}

// --- ESTE ES EL QUE TE FALTABA ---
sealed class PasswordResetState {
    object Idle : PasswordResetState()
    object Loading : PasswordResetState()
    object CodeSent : PasswordResetState()
    object CodeVerified : PasswordResetState()
    object PasswordResetSuccess : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}
// --- VIEWMODEL ---

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    // --- LiveData para Registro ---
    private val _registrationState = MutableLiveData<RegistrationState>(RegistrationState.Idle)
    val registrationState: LiveData<RegistrationState> = _registrationState

    // --- LiveData para Login ---
    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    // --- LiveData para Términos y Condiciones ---
    private val _termsHtml = MutableLiveData<String>()
    val termsHtml: LiveData<String> = _termsHtml

    private val _termsLoading = MutableLiveData<Boolean>()
    val termsLoading: LiveData<Boolean> = _termsLoading

    private val _termsError = MutableLiveData<String?>()
    val termsError: LiveData<String?> = _termsError

    // --- StateFlow para Sesión (Splash/Main) ---
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    // Recuperación de Contraseña
    private val _passwordResetState = MutableLiveData<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState: LiveData<PasswordResetState> = _passwordResetState


    // --- FUNCIONES ---

    // Verifica si hay un token válido al abrir la app
    fun checkSessionStatus() {
        viewModelScope.launch {
            _sessionState.value = SessionState.Loading
            try {
                val token = repository.getToken()
                if (!token.isNullOrBlank()) {
                    _sessionState.value = SessionState.Authenticated
                } else {
                    _sessionState.value = SessionState.Unauthenticated
                }
            } catch (e: Exception) {
                _sessionState.value = SessionState.Unauthenticated
            }
        }
    }

    fun onRegisterClicked(
        nombre: String,
        email: String,
        numero: String,
        pass1: String,
        pass2: String
    ) {
        if (nombre.isBlank() || email.isBlank() || numero.isBlank() || pass1.isBlank()) {
            _registrationState.value = RegistrationState.Error("Todos los campos son obligatorios")
            return
        }
        if (pass1 != pass2) {
            _registrationState.value = RegistrationState.Error("Las contraseñas no coinciden")
            return
        }

        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                val request = RegisterRequest(email, nombre, numero, pass1, pass2)
                repository.registerUser(request)
                _registrationState.value = RegistrationState.Success
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun onLoginClicked(email: String, pass: String, context: Context) {
        if (email.isBlank() || pass.isBlank()) {
            _loginState.value = LoginState.Error("Email y contraseña son obligatorios")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // 1. Login y obtener token
                val token = repository.login(email, pass)

                // 2. Obtener ID del dispositivo
                val deviceId = DeviceHelper.getDeviceIdentifier(context.applicationContext)
                val deviceName = DeviceHelper.getDeviceName()

                // 3. Registrar dispositivo
                repository.registerDevice(token, deviceName, deviceId)

                // Éxito
                _loginState.value = LoginState.Success

            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun loadTerms(code: Int) {
        viewModelScope.launch {
            _termsLoading.value = true
            _termsError.value = null
            try {
                val html = repository.fetchTermsAndConditions(code)
                _termsHtml.value = html
            } catch (e: Exception) {
                _termsError.value = "No se pudieron cargar los términos: ${e.message}"
            } finally {
                _termsLoading.value = false
            }
        }
    }

    fun termsShown() {
        _termsHtml.value = ""
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            try {
                repository.logout()
            } catch (e: Exception) {
                // Error silencioso
            }
        }
    }

    // --- FUNCIONES DE RECUPERACIÓN DE CONTRASEÑA ---

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _passwordResetState.value = PasswordResetState.Loading
            try {
                repository.requestPasswordReset(email)
                _passwordResetState.value = PasswordResetState.CodeSent
            } catch (e: Exception) {
                _passwordResetState.value = PasswordResetState.Error(e.message ?: "Error al enviar código")
            }
        }
    }

    fun verifyResetCode(email: String, code: String) {
        viewModelScope.launch {
            _passwordResetState.value = PasswordResetState.Loading
            try {
                repository.verifyResetCode(email, code)
                _passwordResetState.value = PasswordResetState.CodeVerified
            } catch (e: Exception) {
                _passwordResetState.value = PasswordResetState.Error(e.message ?: "Código inválido")
            }
        }
    }

    fun confirmPasswordReset(email: String, code: String, newPass: String) {
        viewModelScope.launch {
            _passwordResetState.value = PasswordResetState.Loading
            try {
                repository.confirmPasswordReset(email, code, newPass)
                _passwordResetState.value = PasswordResetState.PasswordResetSuccess
            } catch (e: Exception) {
                _passwordResetState.value = PasswordResetState.Error(e.message ?: "Error al cambiar contraseña")
            }
        }
    }

    fun resetState() {
        _passwordResetState.value = PasswordResetState.Idle
    }
}