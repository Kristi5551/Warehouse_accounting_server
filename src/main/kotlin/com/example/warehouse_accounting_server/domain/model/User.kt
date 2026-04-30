package com.example.warehouse_accounting_server.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class User(
    val id: Long,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val role: UserRole,
    val status: UserStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
