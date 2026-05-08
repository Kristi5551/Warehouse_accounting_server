package com.example.warehouse_accounting_server.domain.model.reports

import java.math.BigDecimal

data class LowStockReport(
    val productId: Long,
    val productArticle: String,
    val productName: String,
    val warehouseId: Long,
    val warehouseName: String,
    val quantity: BigDecimal,
    val minStock: BigDecimal,
)
