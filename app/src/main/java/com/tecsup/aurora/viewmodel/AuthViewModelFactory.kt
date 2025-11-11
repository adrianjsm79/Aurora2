package com.tecsup.aurora.viewmodel

import android.app.Application // <-- ¡Importante! Añadir este import
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tecsup.aurora.data.repository.AuthRepository

/**
 * Esta fábrica crea el AuthViewModel y le pasa las dependencias necesarias:
 * - AuthRepository: Para la lógica de negocio y llamadas a la API.
 * - Application: Para que el ViewModel pueda acceder al contexto de forma segura.
 */
class AuthViewModelFactory(
    private val repository: AuthRepository,
    private val application: Application // 1. AÑADIR Application al constructor
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // 2. PASAR ambas dependencias al crear el AuthViewModel
            return AuthViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
