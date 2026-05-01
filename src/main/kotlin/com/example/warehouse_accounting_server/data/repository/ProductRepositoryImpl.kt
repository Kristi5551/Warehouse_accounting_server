package com.example.warehouse_accounting_server.data.repository

import com.example.warehouse_accounting_server.data.mapper.ProductMapper
import com.example.warehouse_accounting_server.data.table.CategoriesTable
import com.example.warehouse_accounting_server.data.table.ProductsTable
import com.example.warehouse_accounting_server.domain.model.Product
import com.example.warehouse_accounting_server.domain.repository.ProductRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

class ProductRepositoryImpl : ProductRepository {

    override fun findAll(search: String?, categoryId: Long?, activeOnly: Boolean): List<Product> = transaction {
        (ProductsTable innerJoin CategoriesTable).selectAll().where {
            val activeCond = if (activeOnly) (ProductsTable.isActive eq true) else Op.TRUE
            val categoryCond = categoryId?.let { ProductsTable.categoryId eq it } ?: Op.TRUE
            val searchCond = search?.let { q ->
                val p = "%${q.trim()}%"
                (ProductsTable.name like p) or (ProductsTable.article like p)
            } ?: Op.TRUE
            activeCond and categoryCond and searchCond
        }.orderBy(ProductsTable.name).map(ProductMapper::toDomain)
    }

    override fun findById(id: Long): Product? = transaction {
        (ProductsTable innerJoin CategoriesTable)
            .selectAll().where { ProductsTable.id eq id }
            .singleOrNull()
            ?.let(ProductMapper::toDomain)
    }

    override fun findByArticle(article: String): Product? = transaction {
        (ProductsTable innerJoin CategoriesTable)
            .selectAll().where { ProductsTable.article eq article }
            .singleOrNull()
            ?.let(ProductMapper::toDomain)
    }

    override fun existsByArticle(article: String, excludeId: Long?): Boolean = transaction {
        val rows = ProductsTable.selectAll()
            .where { ProductsTable.article eq article }
            .toList()
        if (excludeId == null) rows.isNotEmpty()
        else rows.any { it[ProductsTable.id].value != excludeId }
    }

    override fun create(
        article: String,
        name: String,
        categoryId: Long,
        unit: String,
        purchasePrice: BigDecimal,
        salePrice: BigDecimal,
        minStock: BigDecimal,
        now: LocalDateTime,
    ): Product = transaction {
        val id = ProductsTable.insertAndGetId {
            it[ProductsTable.article] = article
            it[ProductsTable.name] = name
            it[ProductsTable.categoryId] = categoryId
            it[ProductsTable.unit] = unit
            it[ProductsTable.purchasePrice] = purchasePrice
            it[ProductsTable.salePrice] = salePrice
            it[ProductsTable.minStock] = minStock
            it[ProductsTable.isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }
        (ProductsTable innerJoin CategoriesTable)
            .selectAll().where { ProductsTable.id eq id }.single()
            .let(ProductMapper::toDomain)
    }

    override fun update(
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
    ): Product? = transaction {
        val n = ProductsTable.update({ ProductsTable.id eq id }) {
            it[ProductsTable.article] = article
            it[ProductsTable.name] = name
            it[ProductsTable.categoryId] = categoryId
            it[ProductsTable.unit] = unit
            it[ProductsTable.purchasePrice] = purchasePrice
            it[ProductsTable.salePrice] = salePrice
            it[ProductsTable.minStock] = minStock
            it[ProductsTable.isActive] = isActive
            it[updatedAt] = now
        }
        if (n == 0) null else findById(id)
    }

    override fun deactivate(id: Long, now: LocalDateTime): Product? = transaction {
        val n = ProductsTable.update({ ProductsTable.id eq id }) {
            it[ProductsTable.isActive] = false
            it[updatedAt] = now
        }
        if (n == 0) null else findById(id)
    }
}
