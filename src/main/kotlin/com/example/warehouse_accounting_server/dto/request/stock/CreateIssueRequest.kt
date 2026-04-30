package com.example.warehouse_accounting_server.dto.request.stock

import kotlinx.serialization.Serializable

@Serializable
data class CreateIssueRequest(
    val warehouseId: Long,
    val productId: Long,
    val quantity: String,
    val reason: String? = null,
    val comment: String? = null,
)
