package com.tecsup.aurora.model

data class Contact(
    val name: String,
    val number: String,
    val photoUri: String?, //peude ser nulo
    var isEmergency: Boolean = false,
    var isTrusted: Boolean = false
)