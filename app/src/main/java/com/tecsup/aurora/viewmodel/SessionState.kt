package com.tecsup.aurora.viewmodel

sealed class SessionState {
    object Loading : SessionState()
    object Authenticated : SessionState()
    object Unauthenticated : SessionState()
}