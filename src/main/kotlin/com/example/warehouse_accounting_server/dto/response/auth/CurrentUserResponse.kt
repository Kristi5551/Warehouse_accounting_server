package com.example.warehouse_accounting_server.dto.response.auth

import com.example.warehouse_accounting_server.dto.response.user.UserResponse
import kotlinx.serialization.Serializable

@Serializable
data class CurrentUserResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String,
    val status: String,
) {
    companion object {
        fun from(u: UserResponse) = CurrentUserResponse(
            id = u.id,
            email = u.email,
            fullName = u.fullName,
            role = u.role,
            status = u.status,
        )
    }
}
