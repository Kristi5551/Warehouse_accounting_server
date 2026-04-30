package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.Warehouse

interface WarehouseRepository {
    fun findById(id: Long): Warehouse?
    fun listActive(): List<Warehouse>
}
