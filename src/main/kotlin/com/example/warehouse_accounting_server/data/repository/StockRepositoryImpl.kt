package com.example.warehouse_accounting_server.data.repository

import com.example.warehouse_accounting_server.config.ConflictException
import com.example.warehouse_accounting_server.data.table.CategoriesTable
import com.example.warehouse_accounting_server.data.table.ProductsTable
import com.example.warehouse_accounting_server.data.table.StockBalancesTable
import com.example.warehouse_accounting_server.data.table.StockOperationItemsTable
import com.example.warehouse_accounting_server.data.table.StockOperationsTable
import com.example.warehouse_accounting_server.data.table.UsersTable
import com.example.warehouse_accounting_server.data.table.WarehousesTable
import com.example.warehouse_accounting_server.domain.model.StockBalance
import com.example.warehouse_accounting_server.domain.model.StockOperation
import com.example.warehouse_accounting_server.domain.model.StockOperationItem
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.StockOperationWithItems
import com.example.warehouse_accounting_server.domain.model.StockStatus
import com.example.warehouse_accounting_server.domain.repository.StockBalanceView
import com.example.warehouse_accounting_server.domain.repository.StockOperationItemView
import com.example.warehouse_accounting_server.domain.repository.StockOperationView
import com.example.warehouse_accounting_server.domain.repository.StockRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

    override fun findOperations(
        type: StockOperationType?,
        productId: Long?,
        userId: Long?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
    ): List<StockOperationView> = transaction {
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
                    val typeCond = type?.let { StockOperationsTable.operationType eq it.name } ?: Op.TRUE
                    val productCond = productId?.let { StockOperationItemsTable.productId eq it } ?: Op.TRUE
                    val userCond = userId?.let { StockOperationsTable.createdBy eq it } ?: Op.TRUE
                    val fromCond =
                        dateFrom?.let { d ->
                            StockOperationsTable.createdAt greaterEq d.atStartOfDay()
                        } ?: Op.TRUE
                    val toCond =
                        dateTo?.let { d ->
                            StockOperationsTable.createdAt lessEq
                                d.atTime(LocalTime.of(23, 59, 59, 999_999_999))
                        } ?: Op.TRUE
                    typeCond and productCond and userCond and fromCond and toCond
                }
                .orderBy(
                    StockOperationsTable.createdAt to SortOrder.DESC,
                    StockOperationsTable.id to SortOrder.DESC,
                    StockOperationItemsTable.id to SortOrder.ASC,
                )
                .toList()
        groupJoinRowsToOperationViews(rows)
    }

    private fun groupJoinRowsToOperationViews(rows: List<ResultRow>): List<StockOperationView> {
        val opOrder = mutableListOf<Long>()
        val grouped = linkedMapOf<Long, MutableList<ResultRow>>()
        for (row in rows) {
            val oid = row[StockOperationsTable.id].value
            if (!grouped.containsKey(oid)) {
                opOrder.add(oid)
            }
            grouped.getOrPut(oid) { mutableListOf() }.add(row)
        }
        return opOrder.map { oid -> buildOperationViewFromJoinRows(grouped.getValue(oid)) }
    }

    private fun buildOperationViewFromJoinRows(rows: List<ResultRow>): StockOperationView {
        val r = rows.first()
        val items =
            rows.map { row ->
                StockOperationItemView(
                    id = row[StockOperationItemsTable.id].value,
                    productId = row[StockOperationItemsTable.productId].value,
                    productArticle = row[ProductsTable.article],
                    productName = row[ProductsTable.name],
                    quantity = row[StockOperationItemsTable.quantity],
                    price = row[StockOperationItemsTable.price],
                    reason = row[StockOperationItemsTable.reason],
                )
            }
        return StockOperationView(
            id = r[StockOperationsTable.id].value,
            operationType = StockOperationType.valueOf(r[StockOperationsTable.operationType]),
            warehouseId = r[StockOperationsTable.warehouseId].value,
            warehouseName = r[WarehousesTable.name],
            createdBy = r[StockOperationsTable.createdBy].value,
            createdByName = r[UsersTable.fullName],
            createdAt = r[StockOperationsTable.createdAt],
            comment = r[StockOperationsTable.comment],
            items = items,
        )
    }

    /**
     * Только внутри уже открытой Exposed-[transaction] (иначе — «No transaction in context»).
     */
    private fun readOperationWithItemsInCurrentTx(operationId: Long): StockOperationWithItems? {
        val row =
            StockOperationsTable.selectAll().where { StockOperationsTable.id eq operationId }.singleOrNull()
                ?: return null
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
        return StockOperationWithItems(op, items)
    }

    override fun findOperationWithItems(operationId: Long): StockOperationWithItems? = transaction {
        readOperationWithItemsInCurrentTx(operationId)
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
    ): StockOperationWithItems = transaction {
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
    ): StockOperationWithItems = transaction {
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
    ): StockOperationWithItems = transaction {
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
    ): StockOperationWithItems = transaction {
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
    ): StockOperationWithItems {
        val current = getBalanceQuantity(productId, warehouseId)
        val newQty = adjust(current, qtyDelta)
        val opId = StockOperationsTable.insertAndGetId {
            it[StockOperationsTable.operationType] = type.name
            it[StockOperationsTable.warehouseId] = warehouseId
            it[StockOperationsTable.createdBy] = userId
            it[StockOperationsTable.createdAt] = now
            it[StockOperationsTable.comment] = comment
        }
        val lineQty = itemQuantityForRow ?: qtyDelta
        val itemId = StockOperationItemsTable.insertAndGetId {
            it[StockOperationItemsTable.operationId] = opId
            it[StockOperationItemsTable.productId] = productId
            it[StockOperationItemsTable.quantity] = lineQty
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
        val op = StockOperation(
            id = opId.value,
            operationType = type,
            warehouseId = warehouseId,
            createdBy = userId,
            createdAt = now,
            comment = comment,
        )
        val item = StockOperationItem(
            id = itemId.value,
            operationId = opId.value,
            productId = productId,
            quantity = lineQty,
            price = price,
            reason = reason,
        )
        return StockOperationWithItems(op, listOf(item))
    }
}
