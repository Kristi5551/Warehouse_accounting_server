package com.example.warehouse_accounting_server.dto.response.health

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val database: String? = null,
    val message: String? = null,
)
