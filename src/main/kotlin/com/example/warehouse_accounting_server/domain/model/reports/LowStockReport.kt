package com.example.warehouse_accounting_server.domain.model.reports

import java.math.BigDecimal

/** Строка отчёта `/api/reports/low-stock` (не операционный `/api/stock/low`). */
data class LowStockReport(
    val productId: Long,
    val productArticle: String,
    val productName: String,
    val warehouseId: Long,
    val warehouseName: String,
    val quantity: BigDecimal,
    val minStock: BigDecimal,
)
