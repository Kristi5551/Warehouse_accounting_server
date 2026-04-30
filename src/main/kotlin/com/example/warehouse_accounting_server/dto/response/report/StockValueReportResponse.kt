package com.example.warehouse_accounting_server.dto.response.report

import kotlinx.serialization.Serializable

@Serializable
data class StockValueReportResponse(
    val warehouseId: Long,
    val warehouseName: String,
    val totalPurchaseValue: String,
    val totalSaleValue: String,
)
