package com.example.warehouse_accounting_server.domain.model

import java.time.LocalDateTime

data class StockOperation(
    val id: Long,
    val operationType: StockOperationType,
    val warehouseId: Long,
    val createdBy: Long,
    val createdAt: LocalDateTime,
    val comment: String?,
)
