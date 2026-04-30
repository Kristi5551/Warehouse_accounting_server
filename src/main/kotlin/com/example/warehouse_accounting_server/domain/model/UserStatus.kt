package com.example.warehouse_accounting_server.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserStatus {
    PENDING,
    ACTIVE,
    BLOCKED,
}
