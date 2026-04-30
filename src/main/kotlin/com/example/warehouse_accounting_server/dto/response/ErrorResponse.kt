package com.example.warehouse_accounting_server.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String,
    val details: String? = null,
)
