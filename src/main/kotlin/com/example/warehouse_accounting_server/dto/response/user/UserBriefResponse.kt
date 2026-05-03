package com.example.warehouse_accounting_server.dto.response.user

import kotlinx.serialization.Serializable

@Serializable
data class UserBriefResponse(
    val id: Long,
    val fullName: String,
)
