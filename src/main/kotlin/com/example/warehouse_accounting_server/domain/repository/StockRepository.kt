package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.StockBalance
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.StockOperationWithItems
import com.example.warehouse_accounting_server.domain.model.StockStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

interface StockRepository {
    fun getBalances(search: String?, categoryId: Long?, status: StockStatus?): List<StockBalanceView>
    fun getLowStock(): List<StockBalanceView>

    fun findBalance(productId: Long, warehouseId: Long): StockBalance?
    fun createBalanceIfMissing(productId: Long, warehouseId: Long, now: LocalDateTime): StockBalance
    fun updateQuantity(productId: Long, warehouseId: Long, quantity: BigDecimal, now: LocalDateTime): StockBalance

    fun findOperations(
        type: StockOperationType?,
        productId: Long?,
        userId: Long?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
    ): List<StockOperationView>

    fun findProductHistory(productId: Long): List<StockOperationView> =
        findOperations(null, productId, null, null, null)

    fun createReceipt(
        warehouseId: Long,
        productId: Long,
        quantity: BigDecimal,
        price: BigDecimal,
        supplier: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperationWithItems

    fun createIssue(
        warehouseId: Long,
        productId: Long,
        quantity: BigDecimal,
        reason: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperationWithItems

    fun createWriteOff(
        warehouseId: Long,
        productId: Long,
        quantity: BigDecimal,
        reason: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperationWithItems

    fun createInventoryAdjustment(
        warehouseId: Long,
        productId: Long,
        actualQuantity: BigDecimal,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperationWithItems

    fun findOperationWithItems(operationId: Long): StockOperationWithItems?
}
