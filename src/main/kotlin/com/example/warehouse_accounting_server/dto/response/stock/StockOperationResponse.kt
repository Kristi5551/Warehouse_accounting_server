package com.example.warehouse_accounting_server.dto.response.stock

import kotlinx.serialization.Serializable

@Serializable
data class StockOperationResponse(
    val id: Long,
    val operationType: String,
    val warehouseId: Long,
    val warehouseName: String?,
    val createdBy: Long,
    val createdByName: String?,
    val createdAt: String,
    val comment: String?,
    val items: List<StockOperationItemResponse>,
)
