package com.tecsup.aurora.model

data class Contact(
    val name: String,
    val number: String,
    val photoUri: String?, //URI de la foto puede ser nulo
    var isEmergency: Boolean = false, //indicador de emergencia
    var isTrusted: Boolean = false   //indicador de confiable
)
