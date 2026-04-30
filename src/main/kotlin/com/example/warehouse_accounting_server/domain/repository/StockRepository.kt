package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.StockBalance
import com.example.warehouse_accounting_server.domain.model.StockOperation
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.StockOperationWithItems
import java.math.BigDecimal
import java.time.LocalDateTime

data class StockHistoryFilter(
    val operationType: StockOperationType? = null,
    val productId: Long? = null,
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
    val userId: Long? = null,
)

interface StockRepository {
    fun listBalances(warehouseId: Long? = null): List<StockBalance>
    fun listLowStock(warehouseId: Long? = null): List<StockBalance>

    fun historyForProduct(productId: Long, filter: StockHistoryFilter): List<StockOperationWithItems>

    fun createReceipt(
        warehouseId: Long,
        productId: Long,
        quantity: BigDecimal,
        price: BigDecimal,
        supplier: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperation

    fun createIssue(
        warehouseId: Long,
        productId: Long,
        quantity: BigDecimal,
        reason: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperation

    fun createWriteOff(
        warehouseId: Long,
        productId: Long,
        quantity: BigDecimal,
        reason: String?,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperation

    fun createInventoryAdjustment(
        warehouseId: Long,
        productId: Long,
        actualQuantity: BigDecimal,
        comment: String?,
        userId: Long,
        now: LocalDateTime,
    ): StockOperation
}
