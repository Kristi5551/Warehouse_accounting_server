package com.example.warehouse_accounting_server.dto.response.stock

import kotlinx.serialization.Serializable

/** JSON для операционного `GET /api/stock/low` и общих списков остатков: категория, статус, дата обновления. */
@Serializable
data class StockBalanceResponse(
    val id: Long,
    val productId: Long,
    val productArticle: String,
    val productName: String,
    val categoryName: String?,
    val warehouseId: Long,
    val warehouseName: String,
    val quantity: String,
    val minStock: String,
    val status: String,
    val updatedAt: String,
)
