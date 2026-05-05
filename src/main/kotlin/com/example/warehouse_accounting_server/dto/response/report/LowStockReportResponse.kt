package com.example.warehouse_accounting_server.dto.response.report

import kotlinx.serialization.Serializable

/** JSON для отчётного `GET /api/reports/low-stock` (строки отчёта; без категории, статуса и `updatedAt`). */
@Serializable
data class LowStockReportResponse(
    val productId: Long,
    val productArticle: String,
    val productName: String,
    val warehouseId: Long,
    val warehouseName: String,
    val quantity: String,
    val minStock: String,
)
