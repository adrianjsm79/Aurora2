package com.tecsup.aurora.data.repository

import com.tecsup.aurora.data.local.UserSession
import com.tecsup.aurora.data.model.LoginRequest
import com.tecsup.aurora.data.model.RegisterRequest
import com.tecsup.aurora.data.remote.ApiService
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import com.tecsup.aurora.data.model.DeviceRequest
import com.tecsup.aurora.data.model.UserProfile
import com.tecsup.aurora.data.model.AddContactRequest
import com.tecsup.aurora.data.model.TrustedContact


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

    //obtener contactos
    suspend fun getTrustedContacts(token: String): List<TrustedContact> {
        val authToken = "Bearer $token"
        val response = apiService.getTrustedContacts(authToken)
        if (!response.isSuccessful) {
            throw Exception("Error al cargar contactos de confianza: ${response.code()}")
        }
        return response.body() ?: emptyList()
    }
    //añadir contactos
    suspend fun addTrustedContact(token: String, numero: String): TrustedContact {
        val authToken = "Bearer $token"
        val response = apiService.addTrustedContact(authToken, AddContactRequest(numero))

        if (response.code() == 404) {
            throw Exception("Usuario no encontrado con ese número.")
        }
        if (response.code() == 400) {
            throw Exception("Contacto ya existe o no puedes agregarte a ti mismo.")
        }
        if (!response.isSuccessful) {
            throw Exception("Error al añadir contacto: ${response.code()}")
        }
        return response.body() ?: throw Exception("Respuesta de contacto vacía")
    }
    //quitar contactos
    suspend fun removeTrustedContact(token: String, contactId: Int) {
        val authToken = "Bearer $token"
        val response = apiService.removeTrustedContact(authToken, contactId)
        if (!response.isSuccessful) {
            throw Exception("Error al eliminar contacto: ${response.code()}")
        }
    }

}