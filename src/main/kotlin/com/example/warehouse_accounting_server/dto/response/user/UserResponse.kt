package com.example.warehouse_accounting_server.dto.response.user

import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val status: UserStatus,
)
