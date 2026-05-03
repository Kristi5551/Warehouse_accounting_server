package com.example.warehouse_accounting_server.dto.response.report

import kotlinx.serialization.Serializable

@Serializable
data class StockValueReportResponse(
    val totalValue: String,
    val items: List<StockValueItemResponse>,
)
