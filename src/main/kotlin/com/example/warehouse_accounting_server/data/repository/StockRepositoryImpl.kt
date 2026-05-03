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
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.vendors.ForUpdateOption
import org.jetbrains.exposed.sql.statements.jdbc.JdbcConnectionImpl
import java.math.BigDecimal
import java.sql.Timestamp
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
        upsertAddToBalance(productId, warehouseId, quantity, now)
        finishMovement(
            type = StockOperationType.RECEIPT,
            warehouseId = warehouseId,
            productId = productId,
            lineQty = quantity,
            price = price,
            reason = null,
            comment = c,
            userId = userId,
            now = now,
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
        val rows =
            tryDecrementBalance(
                productId = productId,
                warehouseId = warehouseId,
                qty = quantity,
                now = now,
            )
        if (rows == 0) {
            throw ConflictException("Недостаточно товара на складе")
        }
        finishMovement(
            type = StockOperationType.ISSUE,
            warehouseId = warehouseId,
            productId = productId,
            lineQty = quantity.negate(),
            price = null,
            reason = reason,
            comment = comment,
            userId = userId,
            now = now,
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
        val rows =
            tryDecrementBalance(
                productId = productId,
                warehouseId = warehouseId,
                qty = quantity,
                now = now,
            )
        if (rows == 0) {
            throw ConflictException("Недостаточно товара на складе")
        }
        finishMovement(
            type = StockOperationType.WRITE_OFF,
            warehouseId = warehouseId,
            productId = productId,
            lineQty = quantity.negate(),
            price = null,
            reason = reason,
            comment = comment,
            userId = userId,
            now = now,
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
        val current = lockBalanceQuantityForUpdate(productId, warehouseId)
        fun fmt(n: BigDecimal): String = n.stripTrailingZeros().toPlainString()
        val delta = actualQuantity.subtract(current)
        val invLine =
            "Инвентаризация: было ${fmt(current)}, стало ${fmt(actualQuantity)}, расхождение ${fmt(delta)}"
        val fullComment =
            if (comment.isNullOrBlank()) invLine else "$invLine. ${comment.trim()}"

        val result =
            finishMovement(
                type = StockOperationType.INVENTORY,
                warehouseId = warehouseId,
                productId = productId,
                lineQty = actualQuantity,
                price = null,
                reason = invLine,
                comment = fullComment,
                userId = userId,
                now = now,
            )
        upsertSetBalanceQuantity(productId, warehouseId, actualQuantity, now)
        result
    }

    /** PostgreSQL: одна команда upsert защищает от гонки «два прихода создают строку». */
    private fun upsertAddToBalance(
        productId: Long,
        warehouseId: Long,
        addQty: BigDecimal,
        now: LocalDateTime,
    ) {
        val jdbc = (TransactionManager.current().connection as JdbcConnectionImpl).connection
        val sql =
            """
            INSERT INTO stock_balances (product_id, warehouse_id, quantity, updated_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (product_id, warehouse_id)
            DO UPDATE SET
                quantity = stock_balances.quantity + EXCLUDED.quantity,
                updated_at = EXCLUDED.updated_at
            """.trimIndent()
        jdbc.prepareStatement(sql).use { ps ->
            ps.setLong(1, productId)
            ps.setLong(2, warehouseId)
            ps.setBigDecimal(3, addQty)
            ps.setTimestamp(4, Timestamp.valueOf(now))
            ps.executeUpdate()
        }
    }

    /** Атомарное списание: условие quantity >= qty исключает lost update и отрицательный остаток. */
    private fun tryDecrementBalance(
        productId: Long,
        warehouseId: Long,
        qty: BigDecimal,
        now: LocalDateTime,
    ): Int =
        StockBalancesTable.update({
            (StockBalancesTable.productId eq productId) and
                (StockBalancesTable.warehouseId eq warehouseId) and
                (StockBalancesTable.quantity greaterEq qty)
        }) {
            it[StockBalancesTable.quantity] = StockBalancesTable.quantity minus qty
            it[StockBalancesTable.updatedAt] = now
        }

    private fun upsertSetBalanceQuantity(
        productId: Long,
        warehouseId: Long,
        newQty: BigDecimal,
        now: LocalDateTime,
    ) {
        val jdbc = (TransactionManager.current().connection as JdbcConnectionImpl).connection
        val sql =
            """
            INSERT INTO stock_balances (product_id, warehouse_id, quantity, updated_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (product_id, warehouse_id)
            DO UPDATE SET
                quantity = EXCLUDED.quantity,
                updated_at = EXCLUDED.updated_at
            """.trimIndent()
        jdbc.prepareStatement(sql).use { ps ->
            ps.setLong(1, productId)
            ps.setLong(2, warehouseId)
            ps.setBigDecimal(3, newQty)
            ps.setTimestamp(4, Timestamp.valueOf(now))
            ps.executeUpdate()
        }
    }

    /**
     * Строка остатка под блокировкой на время транзакции (если есть);
     * иначе логический «ноль» как раньше ([getBalanceQuantity]).
     */
    private fun lockBalanceQuantityForUpdate(productId: Long, warehouseId: Long): BigDecimal {
        val rows =
            StockBalancesTable
                .select(StockBalancesTable.quantity)
                .where {
                    (StockBalancesTable.productId eq productId) and
                        (StockBalancesTable.warehouseId eq warehouseId)
                }
                .forUpdate(ForUpdateOption.ForUpdate)
                .toList()
        return rows.singleOrNull()?.get(StockBalancesTable.quantity) ?: BigDecimal.ZERO
    }

    private fun finishMovement(
        type: StockOperationType,
        warehouseId: Long,
        productId: Long,
        lineQty: BigDecimal,
        price: BigDecimal?,
        reason: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperationWithItems {
        val opId = StockOperationsTable.insertAndGetId {
            it[StockOperationsTable.operationType] = type.name
            it[StockOperationsTable.warehouseId] = warehouseId
            it[StockOperationsTable.createdBy] = userId
            it[StockOperationsTable.createdAt] = now
            it[StockOperationsTable.comment] = comment
        }
        val itemId = StockOperationItemsTable.insertAndGetId {
            it[StockOperationItemsTable.operationId] = opId
            it[StockOperationItemsTable.productId] = productId
            it[StockOperationItemsTable.quantity] = lineQty
            it[StockOperationItemsTable.price] = price
            it[StockOperationItemsTable.reason] = reason
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
