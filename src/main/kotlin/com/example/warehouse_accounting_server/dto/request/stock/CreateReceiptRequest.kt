package com.example.warehouse_accounting_server.dto.request.stock

import kotlinx.serialization.Serializable

@Serializable
data class CreateReceiptRequest(
    val warehouseId: Long,
    val productId: Long,
    val quantity: String,
    val price: String,
    val supplier: String? = null,
    val comment: String? = null,
)
