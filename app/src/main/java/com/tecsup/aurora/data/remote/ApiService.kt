package com.tecsup.aurora.data.remote

import com.tecsup.aurora.data.model.LoginRequest
import com.tecsup.aurora.data.model.LoginResponse
import com.tecsup.aurora.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    // Apunta a tu endpoint de registro en Railway/Django
    @POST("/api/users/register/")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): Response<Void> // Usamos Response<Void> para un 201 Creado sin cuerpo

    // Endpoint de Login
    @POST("/api/users/login/")
    suspend fun loginUser(
        @Body request: LoginRequest
    ): Response<LoginResponse> // Devuelve la respuesta con el token
}