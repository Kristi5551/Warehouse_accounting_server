package com.example.warehouse_accounting_server.dto.response.stock

import kotlinx.serialization.Serializable

@Serializable
data class StockOperationItemResponse(
    val id: Long,
    val operationId: Long,
    val productId: Long,
    val quantity: String,
    val price: String?,
    val reason: String?,
)
