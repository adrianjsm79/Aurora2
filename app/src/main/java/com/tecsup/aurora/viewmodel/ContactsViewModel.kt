package com.tecsup.aurora.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.aurora.data.model.PhoneContact
import com.tecsup.aurora.data.model.TrustedContact
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.data.repository.ContactsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

// Estado de la UI
data class ContactsUiState(
    val isLoading: Boolean = true,
    val allPhoneContacts: List<PhoneContact> = emptyList(), // Lista completa
    val trustedContacts: List<TrustedContact> = emptyList(), // Lista de confianza
    val filteredPhoneContacts: List<PhoneContact> = emptyList(), // Lista para la UI
    val toastMessage: String? = null
)

class ContactsViewModel(
    private val authRepository: AuthRepository,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<ContactsUiState>(ContactsUiState())
    val uiState: LiveData<ContactsUiState> = _uiState

    init {
        loadAllContacts()
    }

    fun loadAllContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true)
            try {
                // Obtener token (asumimos que existe)
                val token = authRepository.getToken()
                if (token == null) throw Exception("Sesión expirada")

                // Cargar ambas listas en paralelo
                val phoneContactsDeferred = viewModelScope.async { contactsRepository.getPhoneContacts() }
                val trustedContactsDeferred = viewModelScope.async { authRepository.getTrustedContacts(token) }

                val phoneContacts = phoneContactsDeferred.await()
                val trustedContacts = trustedContactsDeferred.await()

                _uiState.value = ContactsUiState(
                    isLoading = false,
                    allPhoneContacts = phoneContacts,
                    trustedContacts = trustedContacts,
                    filteredPhoneContacts = phoneContacts // Al inicio, mostrar todos
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(isLoading = false, toastMessage = e.message)
            }
        }
    }

    fun addTrustedContact(number: String) {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: throw Exception("Sesión expirada")

                // Llama a la API (lógica ya está en el backend)
                authRepository.addTrustedContact(token, number)

                // Éxito: recarga todo
                _uiState.value = _uiState.value?.copy(toastMessage = "Contacto añadido")
                loadAllContacts() // Recarga ambas listas

            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(toastMessage = e.message)
            }
        }
    }

    fun removeTrustedContact(contact: TrustedContact) {
        viewModelScope.launch {
            try {
                val token = authRepository.getToken() ?: throw Exception("Sesión expirada")
                authRepository.removeTrustedContact(token, contact.id)

                _uiState.value = _uiState.value?.copy(toastMessage = "Contacto eliminado")
                loadAllContacts() // Recarga

            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(toastMessage = e.message)
            }
        }
    }

    fun filterContacts(query: String) {
        val currentState = _uiState.value ?: return
        val filteredList = if (query.isBlank()) {
            currentState.allPhoneContacts
        } else {
            currentState.allPhoneContacts.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.number.contains(query)
            }
        }
        _uiState.value = currentState.copy(filteredPhoneContacts = filteredList)
    }

    fun toastShown() {
        _uiState.value = _uiState.value?.copy(toastMessage = null)
    }
}


// --- Fábrica para este ViewModel ---
class ContactsViewModelFactory(
    private val authRepository: AuthRepository,
    private val contactsRepository: ContactsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactsViewModel(authRepository, contactsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}