package com.example.warehouse_accounting_server.domain.repository

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Строка остатка с данными товара, категории и склада (для API остатков).
 */
data class StockBalanceView(
    val id: Long,
    val productId: Long,
    val productArticle: String,
    val productName: String,
    val categoryName: String?,
    val warehouseId: Long,
    val warehouseName: String,
    val quantity: BigDecimal,
    val minStock: BigDecimal,
    val updatedAt: LocalDateTime,
)
