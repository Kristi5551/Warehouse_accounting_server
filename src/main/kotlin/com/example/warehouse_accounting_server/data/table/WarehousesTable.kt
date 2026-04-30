package com.example.warehouse_accounting_server.data.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object WarehousesTable : LongIdTable("warehouses") {
    val name = varchar("name", 255).uniqueIndex()
    val address = varchar("address", 255).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
