package com.tecsup.aurora.model

data class Device(
    val id: Int,
    val name: String,
    val linkedDate: String,
    var isVisibleToContacts: Boolean = false
)
