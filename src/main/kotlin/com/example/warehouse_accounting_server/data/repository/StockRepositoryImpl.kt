package com.example.warehouse_accounting_server.data.repository

import com.example.warehouse_accounting_server.config.ConflictException
import com.example.warehouse_accounting_server.data.table.CategoriesTable
import com.example.warehouse_accounting_server.data.table.ProductsTable
import com.example.warehouse_accounting_server.data.table.StockBalancesTable
import com.example.warehouse_accounting_server.data.table.StockOperationItemsTable
import com.example.warehouse_accounting_server.data.table.StockOperationsTable
import com.example.warehouse_accounting_server.data.table.WarehousesTable
import com.example.warehouse_accounting_server.domain.model.StockBalance
import com.example.warehouse_accounting_server.domain.model.StockOperation
import com.example.warehouse_accounting_server.domain.model.StockOperationItem
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.StockOperationWithItems
import com.example.warehouse_accounting_server.domain.model.StockStatus
import com.example.warehouse_accounting_server.domain.repository.StockBalanceView
import com.example.warehouse_accounting_server.domain.repository.StockHistoryFilter
import com.example.warehouse_accounting_server.domain.repository.StockRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime

class StockRepositoryImpl : StockRepository {

    private val balanceJoin =
        StockBalancesTable innerJoin ProductsTable innerJoin CategoriesTable innerJoin WarehousesTable

    private fun ResultRow.toStockBalanceView(): StockBalanceView =
        StockBalanceView(
            id = this[StockBalancesTable.id].value,
            productId = this[StockBalancesTable.productId].value,
            productArticle = this[ProductsTable.article],
            productName = this[ProductsTable.name],
            categoryName = this[CategoriesTable.name],
            warehouseId = this[StockBalancesTable.warehouseId].value,
            warehouseName = this[WarehousesTable.name],
            quantity = this[StockBalancesTable.quantity],
            minStock = this[ProductsTable.minStock],
            updatedAt = this[StockBalancesTable.updatedAt],
        )

    override fun getBalances(search: String?, categoryId: Long?, status: StockStatus?): List<StockBalanceView> = transaction {
        balanceJoin
            .selectAll()
            .where {
                var condition: Op<Boolean> = ProductsTable.isActive eq true
                categoryId?.let { condition = condition and (ProductsTable.categoryId eq it) }
                status?.let { s ->
                    condition = condition and when (s) {
                        StockStatus.OUT_OF_STOCK -> StockBalancesTable.quantity eq BigDecimal.ZERO
                        StockStatus.LOW_STOCK ->
                            (StockBalancesTable.quantity greater BigDecimal.ZERO) and
                                (StockBalancesTable.quantity lessEq ProductsTable.minStock)
                        StockStatus.IN_STOCK -> StockBalancesTable.quantity greater ProductsTable.minStock
                    }
                }
                val q = search?.trim().orEmpty()
                if (q.isNotEmpty()) {
                    val p = "%$q%"
                    condition =
                        condition and (
                            (ProductsTable.name like p) or (ProductsTable.article like p)
                        )
                }
                condition
            }
            .orderBy(ProductsTable.name to SortOrder.ASC)
            .map { it.toStockBalanceView() }
    }

    /** Остаток ≤ min (включая ноль) для активных товаров. */
    override fun getLowStock(): List<StockBalanceView> = transaction {
        balanceJoin
            .selectAll()
            .where {
                (ProductsTable.isActive eq true) and
                    (StockBalancesTable.quantity lessEq ProductsTable.minStock)
            }
            .orderBy(StockBalancesTable.quantity to SortOrder.ASC)
            .map { it.toStockBalanceView() }
    }

    override fun findBalance(productId: Long, warehouseId: Long): StockBalance? = transaction {
        StockBalancesTable.selectAll()
            .where {
                (StockBalancesTable.productId eq productId) and
                    (StockBalancesTable.warehouseId eq warehouseId)
            }
            .singleOrNull()?.let { row ->
                StockBalance(
                    id = row[StockBalancesTable.id].value,
                    productId = row[StockBalancesTable.productId].value,
                    warehouseId = row[StockBalancesTable.warehouseId].value,
                    quantity = row[StockBalancesTable.quantity],
                    updatedAt = row[StockBalancesTable.updatedAt],
                )
            }
    }

    override fun createBalanceIfMissing(productId: Long, warehouseId: Long, now: LocalDateTime): StockBalance = transaction {
        findBalance(productId, warehouseId) ?: run {
            StockBalancesTable.insert {
                it[StockBalancesTable.productId] = productId
                it[StockBalancesTable.warehouseId] = warehouseId
                it[StockBalancesTable.quantity] = BigDecimal.ZERO
                it[StockBalancesTable.updatedAt] = now
            }
            findBalance(productId, warehouseId)!!
        }
    }

    override fun updateQuantity(productId: Long, warehouseId: Long, quantity: BigDecimal, now: LocalDateTime): StockBalance = transaction {
        val existing = StockBalancesTable.selectAll()
            .where {
                (StockBalancesTable.productId eq productId) and
                    (StockBalancesTable.warehouseId eq warehouseId)
            }
            .singleOrNull()
        if (existing == null) {
            StockBalancesTable.insert {
                it[StockBalancesTable.productId] = productId
                it[StockBalancesTable.warehouseId] = warehouseId
                it[StockBalancesTable.quantity] = quantity
                it[StockBalancesTable.updatedAt] = now
            }
        } else {
            StockBalancesTable.update({
                (StockBalancesTable.productId eq productId) and
                    (StockBalancesTable.warehouseId eq warehouseId)
            }) {
                it[StockBalancesTable.quantity] = quantity
                it[StockBalancesTable.updatedAt] = now
            }
        }
        findBalance(productId, warehouseId)!!
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
        opIds.mapNotNull { findOperationWithItems(it) }
    }

    override fun findOperationWithItems(operationId: Long): StockOperationWithItems? = transaction {
        val row =
            StockOperationsTable.selectAll().where { StockOperationsTable.id eq operationId }.singleOrNull()
                ?: return@transaction null
        val op = StockOperation(
            id = row[StockOperationsTable.id].value,
            operationType = StockOperationType.valueOf(row[StockOperationsTable.operationType]),
            warehouseId = row[StockOperationsTable.warehouseId].value,
            createdBy = row[StockOperationsTable.createdBy].value,
            createdAt = row[StockOperationsTable.createdAt],
            comment = row[StockOperationsTable.comment],
        )
        val items = StockOperationItemsTable.selectAll().where { StockOperationItemsTable.operationId eq operationId }.map { r ->
            StockOperationItem(
                id = r[StockOperationItemsTable.id].value,
                operationId = operationId,
                productId = r[StockOperationItemsTable.productId].value,
                quantity = r[StockOperationItemsTable.quantity],
                price = r[StockOperationItemsTable.price],
                reason = r[StockOperationItemsTable.reason],
            )
        }
        StockOperationWithItems(op, items)
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
            if (!supplier.isNullOrBlank()) append("Поставщик: ").append(supplier.trim())
            if (!comment.isNullOrBlank()) {
                if (isNotEmpty()) append("\n")
                append(comment.trim())
            }
        }.ifBlank { null }
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
            itemQuantityForRow = null,
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
                    throw ConflictException("Недостаточно товара на складе")
                }
                next
            },
            itemQuantityForRow = null,
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
                    throw ConflictException("Недостаточно товара на складе")
                }
                next
            },
            itemQuantityForRow = null,
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
        fun fmt(n: BigDecimal): String = n.stripTrailingZeros().toPlainString()
        val invLine =
            "Инвентаризация: было ${fmt(current)}, стало ${fmt(actualQuantity)}, расхождение ${fmt(delta)}"
        val fullComment =
            if (comment.isNullOrBlank()) invLine else "$invLine. ${comment.trim()}"
        createMovement(
            type = StockOperationType.INVENTORY,
            warehouseId = warehouseId,
            productId = productId,
            qtyDelta = delta,
            price = null,
            reason = invLine,
            comment = fullComment,
            userId = userId,
            now = now,
            itemQuantityForRow = actualQuantity,
            adjust = { _, _ -> actualQuantity },
        )
    }

    private fun getBalanceQuantity(productId: Long, warehouseId: Long): BigDecimal {
        val row = StockBalancesTable.selectAll()
            .where {
                (StockBalancesTable.productId eq productId) and
                    (StockBalancesTable.warehouseId eq warehouseId)
            }
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
        itemQuantityForRow: BigDecimal?,
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
            it[StockOperationItemsTable.quantity] = itemQuantityForRow ?: qtyDelta
            it[StockOperationItemsTable.price] = price
            it[StockOperationItemsTable.reason] = reason
        }
        val existing = StockBalancesTable.selectAll()
            .where {
                (StockBalancesTable.productId eq productId) and
                    (StockBalancesTable.warehouseId eq warehouseId)
            }
            .singleOrNull()
        if (existing == null) {
            StockBalancesTable.insert {
                it[StockBalancesTable.productId] = productId
                it[StockBalancesTable.warehouseId] = warehouseId
                it[StockBalancesTable.quantity] = newQty
                it[StockBalancesTable.updatedAt] = now
            }
        } else {
            StockBalancesTable.update({
                (StockBalancesTable.productId eq productId) and
                    (StockBalancesTable.warehouseId eq warehouseId)
            }) {
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
