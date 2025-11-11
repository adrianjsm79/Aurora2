package com.tecsup.aurora.data.remote

import com.tecsup.aurora.data.model.DeviceRequest
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.data.model.LoginRequest
import com.tecsup.aurora.data.model.LoginResponse
import com.tecsup.aurora.data.model.RegisterRequest
import com.tecsup.aurora.data.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header

interface ApiService {

    // Apunta a tu endpoint de registro en Railway/Django
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
    ): Response<UserProfile> // <-- NUEVO

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


}