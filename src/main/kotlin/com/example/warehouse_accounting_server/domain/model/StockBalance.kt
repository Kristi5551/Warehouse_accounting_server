package com.example.warehouse_accounting_server.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class StockBalance(
    val id: Long,
    val productId: Long,
    val warehouseId: Long,
    val quantity: BigDecimal,
    val updatedAt: LocalDateTime,
)
