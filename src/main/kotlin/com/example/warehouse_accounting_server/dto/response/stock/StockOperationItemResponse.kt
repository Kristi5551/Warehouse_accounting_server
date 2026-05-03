package com.example.warehouse_accounting_server.dto.response.stock

import kotlinx.serialization.Serializable

/** Строка операции; [quantity] — положительная величина, направление по типу операции. */
@Serializable
data class StockOperationItemResponse(
    val id: Long,
    val operationId: Long,
    val productId: Long,
    val productArticle: String? = null,
    val productName: String? = null,
    val quantity: String,
    val price: String?,
    val reason: String?,
)
