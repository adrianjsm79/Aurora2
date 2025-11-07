package com.tecsup.aurora.data.model

data class RegisterRequest(
    val email: String,
    val nombre: String,
    val numero: String,
    val password: String,
    val password2: String
)