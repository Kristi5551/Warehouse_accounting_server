package com.example.warehouse_accounting_server.dto.response.report

import kotlinx.serialization.Serializable

@Serializable
data class OperationReportResponse(
    val operationId: Long,
    val operationType: String,
    val warehouseId: Long,
    val warehouseName: String,
    val createdByName: String,
    val createdAt: String,
    val productArticle: String,
    val productName: String,
    val quantity: String,
    val price: String?,
)
