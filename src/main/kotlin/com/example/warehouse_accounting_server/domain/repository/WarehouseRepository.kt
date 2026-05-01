package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.Warehouse
import java.time.LocalDateTime

interface WarehouseRepository {
    fun findById(id: Long): Warehouse?
    fun findByName(name: String): Warehouse?
    fun listActive(): List<Warehouse>
    fun create(name: String, address: String?, now: LocalDateTime): Warehouse
}
