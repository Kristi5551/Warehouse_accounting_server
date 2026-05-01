package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.Product
import java.math.BigDecimal
import java.time.LocalDateTime

interface ProductRepository {
    fun findAll(search: String? = null, categoryId: Long? = null, activeOnly: Boolean = true): List<Product>
    fun findById(id: Long): Product?
    fun findByArticle(article: String): Product?
    fun existsByArticle(article: String, excludeId: Long? = null): Boolean
    fun create(
        article: String,
        name: String,
        categoryId: Long,
        unit: String,
        purchasePrice: BigDecimal,
        salePrice: BigDecimal,
        minStock: BigDecimal,
        now: LocalDateTime,
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
        now: LocalDateTime,
    ): Product?
    fun deactivate(id: Long, now: LocalDateTime): Product?
}
