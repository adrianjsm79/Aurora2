package com.tecsup.aurora.data.model

data class Device(
    val id: Int,
    val name: String,
    val linkedDate: String,
    var isVisibleToContacts: Boolean = false
)
