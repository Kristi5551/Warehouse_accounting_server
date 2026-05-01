package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.Category
import java.time.LocalDateTime

interface CategoryRepository {
    fun findAll(includeInactive: Boolean = false): List<Category>
    fun findById(id: Long): Category?
    fun findByName(name: String): Category?
    fun existsByName(name: String, excludeId: Long? = null): Boolean
    fun create(name: String, description: String?, now: LocalDateTime): Category
    fun update(id: Long, name: String, description: String?, isActive: Boolean, now: LocalDateTime): Category?
    fun deactivate(id: Long, now: LocalDateTime): Category?
}
