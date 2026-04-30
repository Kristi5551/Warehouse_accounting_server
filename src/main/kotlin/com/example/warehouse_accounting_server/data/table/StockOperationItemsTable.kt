package com.example.warehouse_accounting_server.data.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object StockOperationItemsTable : LongIdTable("stock_operation_items") {
    val operationId = reference("operation_id", StockOperationsTable, onDelete = ReferenceOption.CASCADE)
    val productId = reference("product_id", ProductsTable, onDelete = ReferenceOption.RESTRICT)
    val quantity = decimal("quantity", 12, 3)
    val price = decimal("price", 12, 2).nullable()
    val reason = varchar("reason", 255).nullable()
}
