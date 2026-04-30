package com.example.warehouse_accounting_server.dto.response.report

import kotlinx.serialization.Serializable

@Serializable
data class StockSummaryReportResponse(
    val warehouseId: Long,
    val warehouseName: String,
    val productId: Long,
    val productArticle: String,
    val productName: String,
    val quantity: String,
    val unit: String,
)
