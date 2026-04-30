package com.example.warehouse_accounting_server.data.repository

import com.example.warehouse_accounting_server.data.mapper.CategoryMapper
import com.example.warehouse_accounting_server.data.table.CategoriesTable
import com.example.warehouse_accounting_server.domain.model.Category
import com.example.warehouse_accounting_server.domain.repository.CategoryRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class CategoryRepositoryImpl : CategoryRepository {
    override fun findAll(includeInactive: Boolean): List<Category> = transaction {
        if (includeInactive) {
            CategoriesTable.selectAll()
        } else {
            CategoriesTable.selectAll().where { CategoriesTable.isActive eq true }
        }.map(CategoryMapper::toDomain)
    }

    override fun findById(id: Long): Category? = transaction {
        CategoriesTable.selectAll().where { CategoriesTable.id eq id }.singleOrNull()?.let(CategoryMapper::toDomain)
    }

    override fun create(name: String, description: String?, now: LocalDateTime): Category = transaction {
        val id = CategoriesTable.insertAndGetId {
            it[CategoriesTable.name] = name
            it[CategoriesTable.description] = description
            it[CategoriesTable.isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }
        CategoriesTable.selectAll().where { CategoriesTable.id eq id }.single().let(CategoryMapper::toDomain)
    }

    override fun update(id: Long, name: String, description: String?, isActive: Boolean, now: LocalDateTime): Category? = transaction {
        val updated = CategoriesTable.update({ CategoriesTable.id eq id }) {
            it[CategoriesTable.name] = name
            it[CategoriesTable.description] = description
            it[CategoriesTable.isActive] = isActive
            it[updatedAt] = now
        }
        if (updated == 0) null else findById(id)
    }

    override fun deactivate(id: Long, now: LocalDateTime): Boolean = transaction {
        CategoriesTable.update({ CategoriesTable.id eq id }) {
            it[CategoriesTable.isActive] = false
            it[updatedAt] = now
        } > 0
    }
}
