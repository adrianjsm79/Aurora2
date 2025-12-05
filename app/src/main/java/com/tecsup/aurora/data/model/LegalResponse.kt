package com.tecsup.aurora.data.model

data class LegalResponse(
    val code: Int,
    val title: String,
    val content_base64: String,
    val updated_at: String
)