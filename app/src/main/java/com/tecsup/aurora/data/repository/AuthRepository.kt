package com.tecsup.aurora.data.repository

import com.tecsup.aurora.data.local.UserSession
import com.tecsup.aurora.data.model.LoginRequest
import com.tecsup.aurora.data.model.RegisterRequest
import com.tecsup.aurora.data.remote.ApiService
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query


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

    // --- AÑADE ESTA LÓGICA DE LOGIN ---

    suspend fun login(email: String, pass: String) {
        // 1. Llama a la API de Retrofit
        val response = apiService.loginUser(LoginRequest(email, pass))

        if (response.isSuccessful) {
            val loginResponse = response.body()
            val token = loginResponse?.access
            val refreshToken = loginResponse?.refresh

            if (token != null && refreshToken != null) {
                // 2. Si es exitoso, guarda el token en Realm
                saveTokenToRealm(token, refreshToken)
            } else {
                throw Exception("Respuesta de login incompleta")
            }
        } else {
            throw Exception("Credenciales inválidas")
        }
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

    fun getTokenFromRealm(): String? {
        // (Función útil para más adelante)
        return realm.query<UserSession>().first().find()?.token
    }
}