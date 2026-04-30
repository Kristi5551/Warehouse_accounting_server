package com.example.warehouse_accounting_server.data.repository

import com.example.warehouse_accounting_server.data.table.ProductsTable
import com.example.warehouse_accounting_server.data.table.StockBalancesTable
import com.example.warehouse_accounting_server.data.table.StockOperationItemsTable
import com.example.warehouse_accounting_server.data.table.StockOperationsTable
import com.example.warehouse_accounting_server.data.table.UsersTable
import com.example.warehouse_accounting_server.data.table.WarehousesTable
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.reports.LowStockReport
import com.example.warehouse_accounting_server.domain.model.reports.OperationReport
import com.example.warehouse_accounting_server.domain.model.reports.StockSummaryReport
import com.example.warehouse_accounting_server.domain.model.reports.StockValueReport
import com.example.warehouse_accounting_server.domain.repository.ReportRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

class ReportRepositoryImpl : ReportRepository {
    override fun stockSummary(warehouseId: Long?): List<StockSummaryReport> = transaction {
        val join = StockBalancesTable.innerJoin(ProductsTable, { StockBalancesTable.productId }, { ProductsTable.id })
            .innerJoin(WarehousesTable, { StockBalancesTable.warehouseId }, { WarehousesTable.id })
        join.selectAll().where {
            val wh = warehouseId?.let { StockBalancesTable.warehouseId eq it } ?: Op.TRUE
            wh
        }.map { row ->
            StockSummaryReport(
                warehouseId = row[StockBalancesTable.warehouseId].value,
                warehouseName = row[WarehousesTable.name],
                productId = row[ProductsTable.id].value,
                productArticle = row[ProductsTable.article],
                productName = row[ProductsTable.name],
                quantity = row[StockBalancesTable.quantity],
                unit = row[ProductsTable.unit],
            )
        }
    }

    override fun lowStockReport(warehouseId: Long?): List<LowStockReport> = transaction {
        val join = StockBalancesTable.innerJoin(ProductsTable, { StockBalancesTable.productId }, { ProductsTable.id })
            .innerJoin(WarehousesTable, { StockBalancesTable.warehouseId }, { WarehousesTable.id })
        join.selectAll().where {
            val wh = warehouseId?.let { StockBalancesTable.warehouseId eq it } ?: Op.TRUE
            wh and (StockBalancesTable.quantity less ProductsTable.minStock)
        }.map { row ->
            LowStockReport(
                productId = row[ProductsTable.id].value,
                productArticle = row[ProductsTable.article],
                productName = row[ProductsTable.name],
                warehouseId = row[StockBalancesTable.warehouseId].value,
                warehouseName = row[WarehousesTable.name],
                quantity = row[StockBalancesTable.quantity],
                minStock = row[ProductsTable.minStock],
            )
        }
    }

    override fun operationsReport(
        operationType: StockOperationType?,
        productId: Long?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        userId: Long?,
    ): List<OperationReport> = transaction {
        val join = StockOperationsTable
            .innerJoin(WarehousesTable, { StockOperationsTable.warehouseId }, { WarehousesTable.id })
            .innerJoin(UsersTable, { StockOperationsTable.createdBy }, { UsersTable.id })
            .innerJoin(StockOperationItemsTable, { StockOperationsTable.id }, { StockOperationItemsTable.operationId })
            .innerJoin(ProductsTable, { StockOperationItemsTable.productId }, { ProductsTable.id })
        join.selectAll().where {
            val parts = listOfNotNull(
                operationType?.let { StockOperationsTable.operationType eq it.name },
                productId?.let { StockOperationItemsTable.productId eq it },
                from?.let { StockOperationsTable.createdAt greaterEq it },
                to?.let { StockOperationsTable.createdAt lessEq it },
                userId?.let { StockOperationsTable.createdBy eq it },
            )
            if (parts.isEmpty()) Op.TRUE
            else parts.reduce { acc, op -> acc and op }
        }.map { row ->
            OperationReport(
                operationId = row[StockOperationsTable.id].value,
                operationType = StockOperationType.valueOf(row[StockOperationsTable.operationType]),
                warehouseId = row[WarehousesTable.id].value,
                warehouseName = row[WarehousesTable.name],
                createdByName = row[UsersTable.fullName],
                createdAt = row[StockOperationsTable.createdAt],
                productArticle = row[ProductsTable.article],
                productName = row[ProductsTable.name],
                quantity = row[StockOperationItemsTable.quantity],
                price = row[StockOperationItemsTable.price],
            )
        }
    }

    override fun stockValue(warehouseId: Long?): List<StockValueReport> = transaction {
        val join = StockBalancesTable.innerJoin(ProductsTable, { StockBalancesTable.productId }, { ProductsTable.id })
            .innerJoin(WarehousesTable, { StockBalancesTable.warehouseId }, { WarehousesTable.id })
        val rows = join.selectAll().where {
            warehouseId?.let { StockBalancesTable.warehouseId eq it } ?: Op.TRUE
        }.toList()
        rows.groupBy { row -> row[WarehousesTable.id].value to row[WarehousesTable.name] }
            .map { (key, list) ->
                val (whId, whName) = key
                var purchase = BigDecimal.ZERO
                var sale = BigDecimal.ZERO
                list.forEach { row ->
                    val qty = row[StockBalancesTable.quantity]
                    purchase = purchase.add(qty.multiply(row[ProductsTable.purchasePrice]))
                    sale = sale.add(qty.multiply(row[ProductsTable.salePrice]))
                }
                StockValueReport(
                    warehouseId = whId,
                    warehouseName = whName,
                    totalPurchaseValue = purchase,
                    totalSaleValue = sale,
                )
            }
    }
}
