package com.example.warehouse_accounting_server.dto.response.product

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    val id: Long,
    val article: String,
    val name: String,
    val categoryId: Long,
    val categoryName: String?,
    val unit: String,
    val purchasePrice: String,
    val salePrice: String,
    val minStock: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
)
