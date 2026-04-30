package com.example.warehouse_accounting_server.data.repository

import com.example.warehouse_accounting_server.data.table.WarehousesTable
import com.example.warehouse_accounting_server.domain.model.Warehouse
import com.example.warehouse_accounting_server.domain.repository.WarehouseRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class WarehouseRepositoryImpl : WarehouseRepository {
    override fun findById(id: Long): Warehouse? = transaction {
        WarehousesTable.selectAll().where { WarehousesTable.id eq id }.singleOrNull()?.let { row ->
            Warehouse(
                id = row[WarehousesTable.id].value,
                name = row[WarehousesTable.name],
                address = row[WarehousesTable.address],
                isActive = row[WarehousesTable.isActive],
                createdAt = row[WarehousesTable.createdAt],
                updatedAt = row[WarehousesTable.updatedAt],
            )
        }
    }

    override fun listActive(): List<Warehouse> = transaction {
        WarehousesTable.selectAll().where { WarehousesTable.isActive eq true }.map { row ->
            Warehouse(
                id = row[WarehousesTable.id].value,
                name = row[WarehousesTable.name],
                address = row[WarehousesTable.address],
                isActive = row[WarehousesTable.isActive],
                createdAt = row[WarehousesTable.createdAt],
                updatedAt = row[WarehousesTable.updatedAt],
            )
        }
    }
}
