package com.example.warehouse_accounting_server.data.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

object ProductsTable : LongIdTable("products") {
    val article = varchar("article", 100).uniqueIndex()
    val name = varchar("name", 255)
    val categoryId = reference("category_id", CategoriesTable, onDelete = ReferenceOption.RESTRICT)
    val unit = varchar("unit", 50)
    val purchasePrice = decimal("purchase_price", 12, 2)
    val salePrice = decimal("sale_price", 12, 2)
    val minStock = decimal("min_stock", 12, 3)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
