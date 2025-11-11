package com.tecsup.aurora.data.repository

import com.tecsup.aurora.data.local.UserSession
import com.tecsup.aurora.data.model.LoginRequest
import com.tecsup.aurora.data.model.RegisterRequest
import com.tecsup.aurora.data.remote.ApiService
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import com.tecsup.aurora.data.model.DeviceRequest
import com.tecsup.aurora.data.model.UserProfile


// 1. El Repositorio ahora también necesita Realm
class AuthRepository(
    private val apiService: ApiService,
    private val realm: Realm
) {

    // Función de Registro (ya la tenías)
    suspend fun registerUser(request: RegisterRequest) {
        val response = apiService.registerUser(request)
        if (!response.isSuccessful) {
            throw Exception("Error en el registro: ${response.code()}")
        }
    }

    // --- Lógica de Login (Actualizada) ---
    // Ahora devuelve el token para que el siguiente paso lo use
    suspend fun login(email: String, pass: String): String {
        val response = apiService.loginUser(LoginRequest(email, pass))

        if (response.isSuccessful) {
            val loginResponse = response.body()
            val token = loginResponse?.access
            val refreshToken = loginResponse?.refresh

            if (token != null && refreshToken != null) {
                // Guarda el token en Realm
                saveTokenToRealm(token, refreshToken)
                // Devuelve el token para el siguiente paso
                return token
            } else {
                throw Exception("Respuesta de login incompleta")
            }
        } else {
            throw Exception("Credenciales inválidas")
        }
    }


    suspend fun registerDevice(token: String, deviceName: String, deviceId: String) {
        val authToken = "Bearer $token" // Prepara el token para el header
        val request = DeviceRequest(name = deviceName, device_identifier = deviceId)

        val response = apiService.registerDevice(authToken, request)

        if (!response.isSuccessful) {
            // Si falla el registro del dispositivo, el login igual funcionó,
            // pero deberíamos registrar el error.
            throw Exception("Login exitoso, pero falló el registro del dispositivo: ${response.code()}")
        }
        // Si tiene éxito, el dispositivo está registrado/actualizado en el backend
    }


    private suspend fun saveTokenToRealm(token: String, refreshToken: String) {
        // Escribe en la base de datos de Realm
        realm.write {
            // Borra cualquier sesión antigua
            val oldSession = this.query<UserSession>().find()
            delete(oldSession)

            // Guarda la nueva sesión
            copyToRealm(UserSession().apply {
                this.token = token
                this.refreshToken = refreshToken
            })
        }
    }

    // Obtiene el token guardado en Realm
    fun getToken(): String? {
        val session = realm.query<UserSession>().first().find()
        return session?.token
    }

    // Obtiene el perfil del usuario desde la API
    suspend fun getUserProfile(token: String): UserProfile {
        val authToken = "Bearer $token"
        val response = apiService.getUserProfile(authToken)
        if (!response.isSuccessful) {
            throw Exception("Error al cargar el perfil: ${response.code()}")
        }
        return response.body() ?: throw Exception("Respuesta de perfil vacía")
    }

    // Borra la sesión de Realm
    suspend fun logout() {
        realm.write {
            val session = this.query<UserSession>().find()
            delete(session)
        }
    }

}