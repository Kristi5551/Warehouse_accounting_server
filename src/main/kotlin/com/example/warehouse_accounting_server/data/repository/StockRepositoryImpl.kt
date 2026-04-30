package com.example.warehouse_accounting_server.data.repository

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.data.table.ProductsTable
import com.example.warehouse_accounting_server.data.table.StockBalancesTable
import com.example.warehouse_accounting_server.data.table.StockOperationItemsTable
import com.example.warehouse_accounting_server.data.table.StockOperationsTable
import com.example.warehouse_accounting_server.domain.model.StockBalance
import com.example.warehouse_accounting_server.domain.model.StockOperation
import com.example.warehouse_accounting_server.domain.model.StockOperationItem
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.StockOperationWithItems
import com.example.warehouse_accounting_server.domain.repository.StockHistoryFilter
import com.example.warehouse_accounting_server.domain.repository.StockRepository
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

class StockRepositoryImpl : StockRepository {

    override fun listBalances(warehouseId: Long?): List<StockBalance> = transaction {
        var q = StockBalancesTable.selectAll()
        if (warehouseId != null) {
            q = q.andWhere { StockBalancesTable.warehouseId eq warehouseId }
        }
        q.map { row ->
            StockBalance(
                id = row[StockBalancesTable.id].value,
                productId = row[StockBalancesTable.productId].value,
                warehouseId = row[StockBalancesTable.warehouseId].value,
                quantity = row[StockBalancesTable.quantity],
                updatedAt = row[StockBalancesTable.updatedAt],
            )
        }
    }

    override fun listLowStock(warehouseId: Long?): List<StockBalance> = transaction {
        val join = StockBalancesTable innerJoin ProductsTable
        join.selectAll().where {
            val wh = warehouseId?.let { StockBalancesTable.warehouseId eq it } ?: Op.TRUE
            wh and (StockBalancesTable.quantity less ProductsTable.minStock)
        }.map { row ->
            StockBalance(
                id = row[StockBalancesTable.id].value,
                productId = row[StockBalancesTable.productId].value,
                warehouseId = row[StockBalancesTable.warehouseId].value,
                quantity = row[StockBalancesTable.quantity],
                updatedAt = row[StockBalancesTable.updatedAt],
            )
        }
    }

    override fun historyForProduct(productId: Long, filter: StockHistoryFilter): List<StockOperationWithItems> = transaction {
        val join = StockOperationItemsTable innerJoin StockOperationsTable
        val opIds = join.selectAll().where {
            var cond = StockOperationItemsTable.productId eq productId
            filter.operationType?.let { cond = cond and (StockOperationsTable.operationType eq it.name) }
            filter.userId?.let { cond = cond and (StockOperationsTable.createdBy eq it) }
            filter.from?.let { cond = cond and (StockOperationsTable.createdAt greaterEq it) }
            filter.to?.let { cond = cond and (StockOperationsTable.createdAt lessEq it) }
            cond
        }.map { it[StockOperationsTable.id].value }.distinct()
        opIds.mapNotNull { loadOperationWithItems(it) }
    }

    private fun loadOperationWithItems(operationId: Long): StockOperationWithItems? {
        val opRow = StockOperationsTable.selectAll().where { StockOperationsTable.id eq operationId }.singleOrNull() ?: return null
        val op = StockOperation(
            id = opRow[StockOperationsTable.id].value,
            operationType = StockOperationType.valueOf(opRow[StockOperationsTable.operationType]),
            warehouseId = opRow[StockOperationsTable.warehouseId].value,
            createdBy = opRow[StockOperationsTable.createdBy].value,
            createdAt = opRow[StockOperationsTable.createdAt],
            comment = opRow[StockOperationsTable.comment],
        )
        val items = StockOperationItemsTable.selectAll().where { StockOperationItemsTable.operationId eq operationId }.map { row ->
            StockOperationItem(
                id = row[StockOperationItemsTable.id].value,
                operationId = operationId,
                productId = row[StockOperationItemsTable.productId].value,
                quantity = row[StockOperationItemsTable.quantity],
                price = row[StockOperationItemsTable.price],
                reason = row[StockOperationItemsTable.reason],
            )
        }
        return StockOperationWithItems(op, items)
    }

    override fun createReceipt(
        warehouseId: Long,
        productId: Long,
        quantity: BigDecimal,
        price: BigDecimal,
        supplier: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperation = transaction {
        val c = buildString {
            if (supplier != null) append("Supplier: ").append(supplier)
            if (comment != null) {
                if (isNotEmpty()) append("\n")
                append(comment)
            }
        }.ifEmpty { null }
        createMovement(
            type = StockOperationType.RECEIPT,
            warehouseId = warehouseId,
            productId = productId,
            qtyDelta = quantity,
            price = price,
            reason = null,
            comment = c,
            userId = userId,
            now = now,
            adjust = { current, delta -> current + delta },
        )
    }

    override fun createIssue(
        warehouseId: Long,
        productId: Long,
        quantity: BigDecimal,
        reason: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperation = transaction {
        createMovement(
            type = StockOperationType.ISSUE,
            warehouseId = warehouseId,
            productId = productId,
            qtyDelta = quantity.negate(),
            price = null,
            reason = reason,
            comment = comment,
            userId = userId,
            now = now,
            adjust = { current, delta ->
                val next = current + delta
                if (next < BigDecimal.ZERO) {
                    throw ApiException(HttpStatusCode.BadRequest, "Insufficient stock")
                }
                next
            },
        )
    }

    override fun createWriteOff(
        warehouseId: Long,
        productId: Long,
        quantity: BigDecimal,
        reason: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperation = transaction {
        createMovement(
            type = StockOperationType.WRITE_OFF,
            warehouseId = warehouseId,
            productId = productId,
            qtyDelta = quantity.negate(),
            price = null,
            reason = reason,
            comment = comment,
            userId = userId,
            now = now,
            adjust = { current, delta ->
                val next = current + delta
                if (next < BigDecimal.ZERO) {
                    throw ApiException(HttpStatusCode.BadRequest, "Insufficient stock")
                }
                next
            },
        )
    }

    override fun createInventoryAdjustment(
        warehouseId: Long,
        productId: Long,
        actualQuantity: BigDecimal,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperation = transaction {
        val current = getBalanceQuantity(productId, warehouseId)
        val delta = actualQuantity.subtract(current)
        createMovement(
            type = StockOperationType.INVENTORY,
            warehouseId = warehouseId,
            productId = productId,
            qtyDelta = delta,
            price = null,
            reason = null,
            comment = comment,
            userId = userId,
            now = now,
            adjust = { _, _ -> actualQuantity },
        )
    }

    private fun getBalanceQuantity(productId: Long, warehouseId: Long): BigDecimal {
        val row = StockBalancesTable.selectAll()
            .where { (StockBalancesTable.productId eq productId) and (StockBalancesTable.warehouseId eq warehouseId) }
            .singleOrNull()
        return row?.get(StockBalancesTable.quantity) ?: BigDecimal.ZERO
    }

    private fun createMovement(
        type: StockOperationType,
        warehouseId: Long,
        productId: Long,
        qtyDelta: BigDecimal,
        price: BigDecimal?,
        reason: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
        adjust: (BigDecimal, BigDecimal) -> BigDecimal,
    ): StockOperation {
        val current = getBalanceQuantity(productId, warehouseId)
        val newQty = adjust(current, qtyDelta)
        val opId = StockOperationsTable.insertAndGetId {
            it[StockOperationsTable.operationType] = type.name
            it[StockOperationsTable.warehouseId] = warehouseId
            it[StockOperationsTable.createdBy] = userId
            it[StockOperationsTable.createdAt] = now
            it[StockOperationsTable.comment] = comment
        }
        StockOperationItemsTable.insert {
            it[StockOperationItemsTable.operationId] = opId
            it[StockOperationItemsTable.productId] = productId
            it[StockOperationItemsTable.quantity] = qtyDelta
            it[StockOperationItemsTable.price] = price
            it[StockOperationItemsTable.reason] = reason
        }
        val existing = StockBalancesTable.selectAll()
            .where { (StockBalancesTable.productId eq productId) and (StockBalancesTable.warehouseId eq warehouseId) }
            .singleOrNull()
        if (existing == null) {
            StockBalancesTable.insert {
                it[StockBalancesTable.productId] = productId
                it[StockBalancesTable.warehouseId] = warehouseId
                it[StockBalancesTable.quantity] = newQty
                it[StockBalancesTable.updatedAt] = now
            }
        } else {
            StockBalancesTable.update({ (StockBalancesTable.productId eq productId) and (StockBalancesTable.warehouseId eq warehouseId) }) {
                it[StockBalancesTable.quantity] = newQty
                it[StockBalancesTable.updatedAt] = now
            }
        }
        val opRow = StockOperationsTable.selectAll().where { StockOperationsTable.id eq opId }.single()
        return StockOperation(
            id = opRow[StockOperationsTable.id].value,
            operationType = type,
            warehouseId = warehouseId,
            createdBy = userId,
            createdAt = now,
            comment = comment,
        )
    }
}
