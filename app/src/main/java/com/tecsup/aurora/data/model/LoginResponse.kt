package com.tecsup.aurora.data.model

// Coincide con la respuesta del backend (SimpleJWT)
data class LoginResponse(
    val access: String,
    val refresh: String
)