package com.example.warehouse_accounting_server.data.repository

import com.example.warehouse_accounting_server.data.table.WarehousesTable
import com.example.warehouse_accounting_server.domain.model.Warehouse
import com.example.warehouse_accounting_server.domain.repository.WarehouseRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

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

    override fun findByName(name: String): Warehouse? = transaction {
        WarehousesTable.selectAll().where { WarehousesTable.name eq name }.singleOrNull()?.let { row ->
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

    override fun create(name: String, address: String?, now: LocalDateTime): Warehouse = transaction {
        val id = WarehousesTable.insertAndGetId {
            it[WarehousesTable.name] = name
            it[WarehousesTable.address] = address
            it[WarehousesTable.isActive] = true
            it[WarehousesTable.createdAt] = now
            it[WarehousesTable.updatedAt] = now
        }
        findById(id.value)!!
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
