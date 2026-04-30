package com.example.warehouse_accounting_server.data.mapper

import com.example.warehouse_accounting_server.data.table.CategoriesTable
import com.example.warehouse_accounting_server.domain.model.Category
import org.jetbrains.exposed.sql.ResultRow

object CategoryMapper {
    fun toDomain(row: ResultRow): Category =
        Category(
            id = row[CategoriesTable.id].value,
            name = row[CategoriesTable.name],
            description = row[CategoriesTable.description],
            isActive = row[CategoriesTable.isActive],
            createdAt = row[CategoriesTable.createdAt],
            updatedAt = row[CategoriesTable.updatedAt],
        )
}
