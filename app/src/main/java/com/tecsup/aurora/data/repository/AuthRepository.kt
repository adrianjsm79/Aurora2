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
import com.tecsup.aurora.data.model.UpdateProfileRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import android.util.Base64
import kotlinx.coroutines.delay


class AuthRepository(
    private val apiService: ApiService,
    private val realm: Realm
) {

    suspend fun registerUser(request: RegisterRequest) {
        val response = apiService.registerUser(request)
        if (!response.isSuccessful) {
            throw Exception("Error en el registro: ${response.code()}")
        }
    }

    //Lógica de Login
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
        val authToken = "Bearer $token"
        val request = DeviceRequest(name = deviceName, device_identifier = deviceId)

        val response = apiService.registerDevice(authToken, request)

        if (!response.isSuccessful) {
            throw Exception("Login exitoso, pero falló el registro del dispositivo: ${response.code()}")
        }
        // Si tiene éxito, el dispositivo está registrado/actualizado en el backend
    }


    private suspend fun saveTokenToRealm(token: String, refreshToken: String) {
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

    //obtener contactos que confian en el usuario
    suspend fun getTrustedByContacts(token: String): List<TrustedContact> {
        val authToken = "Bearer $token"
        val response = apiService.getTrustedByContacts(authToken)
        if (!response.isSuccessful) {
            throw Exception("Error al cargar lista 'confían en mí': ${response.code()}")
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

    suspend fun updateProfileComplete(
        token: String,
        nombre: String,
        email: String,
        numero: String,
        password: String?,
        imageFile: File?
    ): UserProfile {

        val authToken = "Bearer $token"

        // Convertir textos a RequestBody
        val nombreRB = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailRB = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val numeroRB = numero.toRequestBody("text/plain".toMediaTypeOrNull())

        // Contraseña (solo si el usuario escribió algo)
        val passwordRB = if (!password.isNullOrBlank()) {
            password.toRequestBody("text/plain".toMediaTypeOrNull())
        } else null

        // Imagen (solo si seleccionó una nueva)
        val imagePart = if (imageFile != null) {
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
        } else null

        val response = apiService.updateProfile(authToken, nombreRB, emailRB, numeroRB, passwordRB, imagePart)

        if (!response.isSuccessful) throw Exception("Error: ${response.code()}")
        return response.body() ?: throw Exception("Error")
    }

    //funcion de obteeener los terminos desde el backend
    suspend fun fetchTermsAndConditions(termCode: Int): String {
        val response = apiService.getLegalDocument(termCode)

        if (response.isSuccessful && response.body() != null) {
            val legalDoc = response.body()!!

            // Decodifica el Base64 que viene del servidor
            // El servidor envía el HTML escondido en "content_base64"
            val encodedString = legalDoc.content_base64

            return try {
                val decodedBytes = Base64.decode(encodedString, Base64.DEFAULT)
                String(decodedBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                throw Exception("Error al decodificar el documento legal")
            }
        } else {
            throw Exception("Error al obtener términos: ${response.code()}")
        }
    }

    //recuperar contraseña
    suspend fun requestPasswordReset(email: String) {
        val response = apiService.requestPasswordReset(mapOf("email" to email))
        if (!response.isSuccessful) throw Exception("Error enviando código")
    }

    suspend fun verifyResetCode(email: String, code: String) {
        val response = apiService.verifyResetCode(mapOf("email" to email, "code" to code))
        if (!response.isSuccessful) throw Exception("Código inválido")
    }

    suspend fun confirmPasswordReset(email: String, code: String, newPass: String) {
        val response = apiService.confirmPasswordReset(
            mapOf("email" to email, "code" to code, "new_password" to newPass)
        )
        if (!response.isSuccessful) throw Exception("Error al cambiar contraseña")
    }

}