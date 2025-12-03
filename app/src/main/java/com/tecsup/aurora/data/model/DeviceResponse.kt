package com.tecsup.aurora.data.model

data class DeviceResponse(
    val id: Int,
    val user: Int,
    val name: String,
    val device_identifier: String,
    val is_lost: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val last_seen: String,
    val user_email: String?,
    val photoUrl: String?
)