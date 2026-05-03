package com.example.warehouse_accounting_server.dto.request.user

import kotlinx.serialization.Serializable

@Serializable
data class CreateAdminUserRequest(
    val fullName: String,
    val email: String,
    val password: String,
)
