package com.example.warehouse_accounting_server.dto.response.user

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String,
    val status: String,
)
