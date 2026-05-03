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
import com.example.warehouse_accounting_server.domain.model.reports.OperationReportItemLine
import com.example.warehouse_accounting_server.domain.model.reports.StockValueItem
import com.example.warehouse_accounting_server.domain.repository.ReportRepository
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalTime

class ReportRepositoryImpl : ReportRepository {
    override fun lowStockReport(warehouseId: Long?): List<LowStockReport> = transaction {
        val join =
            StockBalancesTable.innerJoin(ProductsTable, { StockBalancesTable.productId }, { ProductsTable.id })
                .innerJoin(WarehousesTable, { StockBalancesTable.warehouseId }, { WarehousesTable.id })
        join
            .selectAll()
            .where {
                val wh = warehouseId?.let { StockBalancesTable.warehouseId eq it } ?: Op.TRUE
                wh and
                    (ProductsTable.isActive eq true) and
                    (StockBalancesTable.quantity lessEq ProductsTable.minStock)
            }
            .map { row ->
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

    override fun operationsReport(dateFrom: LocalDate?, dateTo: LocalDate?): List<OperationReport> = transaction {
        val join =
            StockOperationsTable
                .innerJoin(WarehousesTable, { StockOperationsTable.warehouseId }, { WarehousesTable.id })
                .innerJoin(UsersTable, { StockOperationsTable.createdBy }, { UsersTable.id })
                .innerJoin(StockOperationItemsTable, { StockOperationsTable.id }, { StockOperationItemsTable.operationId })
                .innerJoin(ProductsTable, { StockOperationItemsTable.productId }, { ProductsTable.id })
        val rows =
            join
                .selectAll()
                .where {
                    val fromCond =
                        dateFrom?.let { d ->
                            StockOperationsTable.createdAt greaterEq d.atStartOfDay()
                        } ?: Op.TRUE
                    val toCond =
                        dateTo?.let { d ->
                            StockOperationsTable.createdAt lessEq d.atTime(LocalTime.of(23, 59, 59, 999_999_999))
                        } ?: Op.TRUE
                    fromCond and toCond
                }
                .orderBy(
                    StockOperationsTable.createdAt to SortOrder.DESC,
                    StockOperationsTable.id to SortOrder.DESC,
                    StockOperationItemsTable.id to SortOrder.ASC,
                )
                .toList()
        groupRowsToOperationReports(rows)
    }

    private fun groupRowsToOperationReports(rows: List<ResultRow>): List<OperationReport> {
        val opOrder = mutableListOf<Long>()
        val grouped = linkedMapOf<Long, MutableList<ResultRow>>()
        for (row in rows) {
            val oid = row[StockOperationsTable.id].value
            if (!grouped.containsKey(oid)) {
                opOrder.add(oid)
            }
            grouped.getOrPut(oid) { mutableListOf() }.add(row)
        }
        return opOrder.map { oid -> buildOperationReport(grouped.getValue(oid)) }
    }

    private fun buildOperationReport(rows: List<ResultRow>): OperationReport {
        val r = rows.first()
        val items =
            rows.map { row ->
                OperationReportItemLine(
                    productArticle = row[ProductsTable.article],
                    productName = row[ProductsTable.name],
                    quantity = row[StockOperationItemsTable.quantity],
                    price = row[StockOperationItemsTable.price],
                )
            }
        return OperationReport(
            operationId = r[StockOperationsTable.id].value,
            operationType = StockOperationType.valueOf(r[StockOperationsTable.operationType]),
            warehouseId = r[WarehousesTable.id].value,
            warehouseName = r[WarehousesTable.name],
            createdByName = r[UsersTable.fullName],
            createdAt = r[StockOperationsTable.createdAt],
            items = items,
        )
    }

    override fun stockValueLines(warehouseId: Long?): List<StockValueItem> = transaction {
        val join =
            StockBalancesTable.innerJoin(ProductsTable, { StockBalancesTable.productId }, { ProductsTable.id })
        join
            .selectAll()
            .where {
                val wh = warehouseId?.let { StockBalancesTable.warehouseId eq it } ?: Op.TRUE
                wh and (ProductsTable.isActive eq true)
            }
            .map { row ->
                val qty = row[StockBalancesTable.quantity]
                val price = row[ProductsTable.purchasePrice]
                StockValueItem(
                    productId = row[ProductsTable.id].value,
                    productArticle = row[ProductsTable.article],
                    productName = row[ProductsTable.name],
                    quantity = qty,
                    purchasePrice = price,
                    value = qty.multiply(price),
                )
            }
    }
}
