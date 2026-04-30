package com.example.warehouse_accounting_server.domain.model

import java.time.LocalDateTime

data class Warehouse(
    val id: Long,
    val name: String,
    val address: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
