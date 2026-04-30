package com.example.warehouse_accounting_server.dto.request.stock

import kotlinx.serialization.Serializable

@Serializable
data class CreateInventoryRequest(
    val warehouseId: Long,
    val productId: Long,
    val actualQuantity: String,
    val comment: String? = null,
)
