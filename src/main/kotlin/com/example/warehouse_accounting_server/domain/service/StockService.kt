package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.data.mapper.toBalanceResponse
import com.example.warehouse_accounting_server.data.mapper.toItemResponse
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.StockStatus
import com.example.warehouse_accounting_server.domain.repository.ProductRepository
import com.example.warehouse_accounting_server.domain.repository.StockHistoryFilter
import com.example.warehouse_accounting_server.domain.repository.StockRepository
import com.example.warehouse_accounting_server.domain.repository.WarehouseRepository
import com.example.warehouse_accounting_server.domain.validation.StockOperationValidator
import com.example.warehouse_accounting_server.dto.request.stock.CreateInventoryRequest
import com.example.warehouse_accounting_server.dto.request.stock.CreateIssueRequest
import com.example.warehouse_accounting_server.dto.request.stock.CreateReceiptRequest
import com.example.warehouse_accounting_server.dto.request.stock.CreateWriteOffRequest
import com.example.warehouse_accounting_server.dto.response.stock.StockBalanceResponse
import com.example.warehouse_accounting_server.dto.response.stock.StockOperationResponse
import com.example.warehouse_accounting_server.util.DateTimeProvider
import io.ktor.http.HttpStatusCode
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StockService(
    private val stockRepository: StockRepository,
    private val productRepository: ProductRepository,
    private val warehouseRepository: WarehouseRepository,
    private val userRepository: com.example.warehouse_accounting_server.domain.repository.UserRepository,
    private val stockOperationValidator: StockOperationValidator,
    private val dateTime: DateTimeProvider,
) {
    fun balances(warehouseId: Long?): List<StockBalanceResponse> {
        val rows = stockRepository.listBalances(warehouseId)
        return rows.map { b ->
            val product = productRepository.findById(b.productId)
                ?: throw ApiException(HttpStatusCode.InternalServerError, "Product missing for balance")
            val warehouse = warehouseRepository.findById(b.warehouseId)
                ?: throw ApiException(HttpStatusCode.InternalServerError, "Warehouse missing for balance")
            val status = when {
                b.quantity <= BigDecimal.ZERO -> StockStatus.OUT_OF_STOCK
                b.quantity < product.minStock -> StockStatus.LOW_STOCK
                else -> StockStatus.IN_STOCK
            }
            b.toBalanceResponse(
                productArticle = product.article,
                productName = product.name,
                categoryName = product.categoryName,
                warehouseName = warehouse.name,
                minStock = product.minStock,
                status = status,
            )
        }
    }

    fun lowStock(warehouseId: Long?): List<StockBalanceResponse> {
        val rows = stockRepository.listLowStock(warehouseId)
        return rows.map { b ->
            val product = productRepository.findById(b.productId)
                ?: throw ApiException(HttpStatusCode.InternalServerError, "Product missing for balance")
            val warehouse = warehouseRepository.findById(b.warehouseId)
                ?: throw ApiException(HttpStatusCode.InternalServerError, "Warehouse missing for balance")
            b.toBalanceResponse(
                productArticle = product.article,
                productName = product.name,
                categoryName = product.categoryName,
                warehouseName = warehouse.name,
                minStock = product.minStock,
                status = StockStatus.LOW_STOCK,
            )
        }
    }

    fun productHistory(
        productId: Long,
        operationType: StockOperationType?,
        from: String?,
        to: String?,
        userId: Long?,
    ): List<StockOperationResponse> {
        val filter = StockHistoryFilter(
            operationType = operationType,
            productId = null,
            from = from?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) },
            to = to?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) },
            userId = userId,
        )
        val ops = stockRepository.historyForProduct(productId, filter)
        return ops.map { enrichOperation(it.operation, it.items) }
    }

    private fun enrichOperation(
        op: com.example.warehouse_accounting_server.domain.model.StockOperation,
        items: List<com.example.warehouse_accounting_server.domain.model.StockOperationItem>,
    ): StockOperationResponse {
        val warehouse = warehouseRepository.findById(op.warehouseId)
        val user = userRepository.findById(op.createdBy)
        return StockOperationResponse(
            id = op.id,
            operationType = op.operationType.name,
            warehouseId = op.warehouseId,
            warehouseName = warehouse?.name,
            createdBy = op.createdBy,
            createdByName = user?.fullName,
            createdAt = op.createdAt.toString(),
            comment = op.comment,
            items = items.map { it.toItemResponse() },
        )
    }

    fun receipt(request: CreateReceiptRequest, userId: Long) {
        val qty = stockOperationValidator.parseQuantity(request.quantity)
        val price = stockOperationValidator.parseMoney(request.price, "price")
        stockRepository.createReceipt(
            warehouseId = request.warehouseId,
            productId = request.productId,
            quantity = qty,
            price = price,
            supplier = request.supplier,
            comment = request.comment,
            userId = userId,
            now = dateTime.now(),
        )
    }

    fun issue(request: CreateIssueRequest, userId: Long) {
        val qty = stockOperationValidator.parseQuantity(request.quantity)
        stockRepository.createIssue(
            warehouseId = request.warehouseId,
            productId = request.productId,
            quantity = qty,
            reason = request.reason,
            comment = request.comment,
            userId = userId,
            now = dateTime.now(),
        )
    }

    fun writeOff(request: CreateWriteOffRequest, userId: Long) {
        val qty = stockOperationValidator.parseQuantity(request.quantity)
        stockRepository.createWriteOff(
            warehouseId = request.warehouseId,
            productId = request.productId,
            quantity = qty,
            reason = request.reason,
            comment = request.comment,
            userId = userId,
            now = dateTime.now(),
        )
    }

    fun inventory(request: CreateInventoryRequest, userId: Long) {
        val qty = stockOperationValidator.parseMoney(request.actualQuantity, "actualQuantity")
        stockRepository.createInventoryAdjustment(
            warehouseId = request.warehouseId,
            productId = request.productId,
            actualQuantity = qty,
            comment = request.comment,
            userId = userId,
            now = dateTime.now(),
        )
    }
}
