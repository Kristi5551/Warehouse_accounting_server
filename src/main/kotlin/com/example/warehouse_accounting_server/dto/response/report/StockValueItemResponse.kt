package com.example.warehouse_accounting_server.dto.response.report

import kotlinx.serialization.Serializable

@Serializable
data class StockValueItemResponse(
    val productId: Long,
    val productArticle: String,
    val productName: String,
    val quantity: String,
    val purchasePrice: String,
    val value: String,
)
