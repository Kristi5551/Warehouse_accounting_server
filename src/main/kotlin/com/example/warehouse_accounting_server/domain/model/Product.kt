package com.example.warehouse_accounting_server.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Product(
    val id: Long,
    val article: String,
    val name: String,
    val categoryId: Long,
    val categoryName: String?,
    val unit: String,
    val purchasePrice: BigDecimal,
    val salePrice: BigDecimal,
    val minStock: BigDecimal,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
