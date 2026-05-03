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
    /** Первая позиция; для обратной совместимости клиентов без [items]. */
    val productArticle: String,
    val productName: String,
    val quantity: String,
    val price: String?,
    /** Все позиции операции (обычно одна; при нескольких — полный список). */
    val items: List<OperationReportItemResponse> = emptyList(),
)
