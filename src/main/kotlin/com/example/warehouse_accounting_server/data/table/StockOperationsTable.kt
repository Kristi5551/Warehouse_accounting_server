package com.example.warehouse_accounting_server.data.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

object StockOperationsTable : LongIdTable("stock_operations") {
    val operationType = varchar("operation_type", 50)
    val warehouseId = reference("warehouse_id", WarehousesTable, onDelete = ReferenceOption.RESTRICT)
    val createdBy = reference("created_by", UsersTable, onDelete = ReferenceOption.RESTRICT)
    val createdAt = datetime("created_at")
    val comment = text("comment").nullable()
}
