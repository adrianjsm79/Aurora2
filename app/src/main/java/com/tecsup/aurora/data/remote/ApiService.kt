package com.tecsup.aurora.data.remote

import com.tecsup.aurora.data.model.DeviceRequest
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.data.model.AddContactRequest
import com.tecsup.aurora.data.model.TrustedContact
import com.tecsup.aurora.data.model.LoginRequest
import com.tecsup.aurora.data.model.LoginResponse
import com.tecsup.aurora.data.model.RegisterRequest
import com.tecsup.aurora.data.model.UpdateProfileRequest
import com.tecsup.aurora.data.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ApiService {

    @POST("/api/users/register/")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): Response<Void>

    @POST("/api/users/login/")
    suspend fun loginUser(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("/api/users/profile/")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfile>

    // Endpoint para registrar un dispositivo
    @POST("/api/devices/") // Apunta al 'create' de tu DeviceViewSet
    suspend fun registerDevice(
        @Header("Authorization") token: String, // El token de sesión
        @Body request: DeviceRequest
    ): Response<DeviceResponse>

    @GET("/api/devices/")
    suspend fun getDevices(
        @Header("Authorization") token: String
    ): Response<List<DeviceResponse>>

    @PATCH("/api/devices/{id}/")
    suspend fun updateDevice(
        @Header("Authorization") token: String,
        @Path("id") deviceId: Int,
        // Añadimos @JvmSuppressWildcards para que Retrofit entienda el 'Any'
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<DeviceResponse>
    // ---------------------------

    @DELETE("/api/devices/{id}/")
    suspend fun deleteDevice(
        @Header("Authorization") token: String,
        @Path("id") deviceId: Int
    ): Response<Void>

    // Endpoints para contactos confiables
    @GET("/api/users/trusted-contacts/")
    suspend fun getTrustedContacts(
        @Header("Authorization") token: String
    ): Response<List<TrustedContact>>

    @POST("/api/users/trusted-contacts/add/")
    suspend fun addTrustedContact(
        @Header("Authorization") token: String,
        @Body request: AddContactRequest
    ): Response<TrustedContact>

    @DELETE("/api/users/trusted-contacts/{id}/remove/")
    suspend fun removeTrustedContact(
        @Header("Authorization") token: String,
        @Path("id") contactId: Int
    ): Response<Void>

    @GET("/api/users/trusted-contacts/trusted-by/")
    suspend fun getTrustedByContacts(
        @Header("Authorization") token: String
    ): Response<List<TrustedContact>>

    @PATCH("/api/users/profile/")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<UserProfile>

}