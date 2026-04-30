package com.example.warehouse_accounting_server.dto.request.product

import kotlinx.serialization.Serializable

@Serializable
data class CreateProductRequest(
    val article: String,
    val name: String,
    val categoryId: Long,
    val unit: String,
    val purchasePrice: String,
    val salePrice: String,
    val minStock: String,
)
