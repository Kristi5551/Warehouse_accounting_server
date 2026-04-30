package com.example.warehouse_accounting_server.domain.model.reports

import java.math.BigDecimal

data class StockValueReport(
    val warehouseId: Long,
    val warehouseName: String,
    val totalPurchaseValue: BigDecimal,
    val totalSaleValue: BigDecimal,
)
