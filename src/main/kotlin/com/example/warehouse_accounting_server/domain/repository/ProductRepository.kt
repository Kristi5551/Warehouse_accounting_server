package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.Product
import java.math.BigDecimal

interface ProductRepository {
    fun findAll(
        categoryId: Long? = null,
        search: String? = null,
        includeInactive: Boolean = false,
    ): List<Product>

    fun findById(id: Long): Product?
    fun create(
        article: String,
        name: String,
        categoryId: Long,
        unit: String,
        purchasePrice: BigDecimal,
        salePrice: BigDecimal,
        minStock: BigDecimal,
        now: java.time.LocalDateTime,
    ): Product

    fun update(
        id: Long,
        article: String,
        name: String,
        categoryId: Long,
        unit: String,
        purchasePrice: BigDecimal,
        salePrice: BigDecimal,
        minStock: BigDecimal,
        isActive: Boolean,
        now: java.time.LocalDateTime,
    ): Product?

    fun deactivate(id: Long, now: java.time.LocalDateTime): Boolean
}
