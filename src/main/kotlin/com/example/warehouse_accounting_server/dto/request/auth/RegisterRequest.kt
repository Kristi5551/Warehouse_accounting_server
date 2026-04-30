package com.example.warehouse_accounting_server.dto.request.auth

import com.example.warehouse_accounting_server.domain.model.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val requestedRole: UserRole,
)
