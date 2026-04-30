package com.example.warehouse_accounting_server.domain.model

import java.time.LocalDateTime

data class Category(
    val id: Long,
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
