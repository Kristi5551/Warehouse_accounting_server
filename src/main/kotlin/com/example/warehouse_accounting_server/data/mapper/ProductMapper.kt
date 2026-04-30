package com.example.warehouse_accounting_server.data.mapper

import com.example.warehouse_accounting_server.data.table.CategoriesTable
import com.example.warehouse_accounting_server.data.table.ProductsTable
import com.example.warehouse_accounting_server.domain.model.Product
import org.jetbrains.exposed.sql.ResultRow

object ProductMapper {
    fun toDomain(row: ResultRow): Product =
        Product(
            id = row[ProductsTable.id].value,
            article = row[ProductsTable.article],
            name = row[ProductsTable.name],
            categoryId = row[ProductsTable.categoryId].value,
            categoryName = runCatching { row[CategoriesTable.name] }.getOrNull(),
            unit = row[ProductsTable.unit],
            purchasePrice = row[ProductsTable.purchasePrice],
            salePrice = row[ProductsTable.salePrice],
            minStock = row[ProductsTable.minStock],
            isActive = row[ProductsTable.isActive],
            createdAt = row[ProductsTable.createdAt],
            updatedAt = row[ProductsTable.updatedAt],
        )
}
