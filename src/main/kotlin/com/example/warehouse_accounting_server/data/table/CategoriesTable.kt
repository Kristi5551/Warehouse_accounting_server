package com.example.warehouse_accounting_server.data.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object CategoriesTable : LongIdTable("categories") {
    val name = varchar("name", 255).uniqueIndex()
    val description = text("description").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
