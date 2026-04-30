package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.Category

interface CategoryRepository {
    fun findAll(includeInactive: Boolean = false): List<Category>
    fun findById(id: Long): Category?
    fun create(name: String, description: String?, now: java.time.LocalDateTime): Category
    fun update(id: Long, name: String, description: String?, isActive: Boolean, now: java.time.LocalDateTime): Category?
    fun deactivate(id: Long, now: java.time.LocalDateTime): Boolean
}
