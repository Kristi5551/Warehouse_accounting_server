package com.example.warehouse_accounting_server.dto.response.auth

import com.example.warehouse_accounting_server.dto.response.user.UserResponse
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse,
)
