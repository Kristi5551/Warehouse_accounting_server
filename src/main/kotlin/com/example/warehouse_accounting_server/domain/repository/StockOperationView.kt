package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.StockOperationType
import java.math.BigDecimal
import java.time.LocalDateTime

/** [quantity] — величина строки (>= 0); направление движения задаёт тип операции. */
data class StockOperationItemView(
    val id: Long,
    val productId: Long,
    val productArticle: String,
    val productName: String,
    val quantity: BigDecimal,
    val price: BigDecimal?,
    val reason: String?,
)

data class StockOperationView(
    val id: Long,
    val operationType: StockOperationType,
    val warehouseId: Long,
    val warehouseName: String,
    val createdBy: Long,
    val createdByName: String,
    val createdAt: LocalDateTime,
    val comment: String?,
    val items: List<StockOperationItemView>,
)
