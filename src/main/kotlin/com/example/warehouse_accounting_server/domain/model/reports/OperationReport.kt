package com.example.warehouse_accounting_server.domain.model.reports

import com.example.warehouse_accounting_server.domain.model.StockOperationType
import java.math.BigDecimal
import java.time.LocalDateTime

data class OperationReport(
    val operationId: Long,
    val operationType: StockOperationType,
    val warehouseId: Long,
    val warehouseName: String,
    val createdByName: String,
    val createdAt: LocalDateTime,
    val productArticle: String,
    val productName: String,
    val quantity: BigDecimal,
    val price: BigDecimal?,
)
