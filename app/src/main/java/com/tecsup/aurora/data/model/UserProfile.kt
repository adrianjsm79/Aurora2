package com.tecsup.aurora.data.model

/**
 * Representa la respuesta JSON de /api/users/profile/
 * Basado en UserSerializer de django
 */
data class UserProfile(
    val id: Int,
    val email: String,
    val nombre: String,
    val numero: String,
    val image: String?,
    val browser_latitude: Double?,    // El '?' lo hace nulable
    val browser_longitude: Double?,
    val browser_last_seen: String?
)