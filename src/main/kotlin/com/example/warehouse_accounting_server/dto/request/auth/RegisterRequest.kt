package com.example.warehouse_accounting_server.dto.request.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val requestedRole: String,
)
