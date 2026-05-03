package com.example.warehouse_accounting_server.dto.response.report

import com.example.warehouse_accounting_server.dto.response.stock.StockBalanceResponse
import kotlinx.serialization.Serializable

@Serializable
data class StockSummaryReportResponse(
    val totalProducts: Int,
    val inStockCount: Int,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val balances: List<StockBalanceResponse>,
)
