package com.example.warehouse_accounting_server.domain.model.reports

import java.math.BigDecimal

data class StockSummaryReport(
    val warehouseId: Long,
    val warehouseName: String,
    val productId: Long,
    val productArticle: String,
    val productName: String,
    val quantity: BigDecimal,
    val unit: String,
)
