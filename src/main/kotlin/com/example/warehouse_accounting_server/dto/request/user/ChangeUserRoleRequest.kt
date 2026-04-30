package com.example.warehouse_accounting_server.dto.request.user

import kotlinx.serialization.Serializable

@Serializable
data class ChangeUserRoleRequest(
    val role: String,
)
