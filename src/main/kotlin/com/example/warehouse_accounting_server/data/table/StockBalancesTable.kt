package com.example.warehouse_accounting_server.data.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

object StockBalancesTable : LongIdTable("stock_balances") {
    val productId = reference("product_id", ProductsTable, onDelete = ReferenceOption.RESTRICT)
    val warehouseId = reference("warehouse_id", WarehousesTable, onDelete = ReferenceOption.RESTRICT)
    val quantity = decimal("quantity", 12, 3)
    val updatedAt = datetime("updated_at")

    init {
        uniqueIndex(productId, warehouseId)
    }
}
