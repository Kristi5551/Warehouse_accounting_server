package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.config.NotFoundException
import com.example.warehouse_accounting_server.config.ValidationException
import com.example.warehouse_accounting_server.data.mapper.toBalanceResponse
import com.example.warehouse_accounting_server.data.mapper.toItemResponse
import com.example.warehouse_accounting_server.domain.model.StockOperation
import com.example.warehouse_accounting_server.domain.model.StockOperationItem
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.StockStatus
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.ProductRepository
import com.example.warehouse_accounting_server.domain.repository.StockHistoryFilter
import com.example.warehouse_accounting_server.domain.repository.StockRepository
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.domain.repository.WarehouseRepository
import com.example.warehouse_accounting_server.domain.validation.StockOperationValidator
import com.example.warehouse_accounting_server.dto.request.stock.CreateInventoryRequest
import com.example.warehouse_accounting_server.dto.request.stock.CreateIssueRequest
import com.example.warehouse_accounting_server.dto.request.stock.CreateReceiptRequest
import com.example.warehouse_accounting_server.dto.request.stock.CreateWriteOffRequest
import com.example.warehouse_accounting_server.dto.response.stock.StockBalanceResponse
import com.example.warehouse_accounting_server.dto.response.stock.StockOperationResponse
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.RoleAccess
import io.ktor.http.HttpStatusCode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StockService(
    private val stockRepository: StockRepository,
    private val productRepository: ProductRepository,
    private val warehouseRepository: WarehouseRepository,
    private val userRepository: UserRepository,
    private val stockOperationValidator: StockOperationValidator,
    private val dateTime: DateTimeProvider,
) {
    companion object {
        private const val STOCK_ITEM_REASON_MAX_LEN = 255
    }

    fun getBalances(
        currentUserId: Long,
        search: String?,
        categoryId: Long?,
        status: StockStatus?,
    ): List<StockBalanceResponse> {
        ensureStockReader(currentUserId)
        return stockRepository.getBalances(search, categoryId, status).map { it.toBalanceResponse() }
    }

    fun getLowStock(currentUserId: Long): List<StockBalanceResponse> {
        ensureStockReader(currentUserId)
        return stockRepository.getLowStock().map { it.toBalanceResponse() }
    }

    private fun ensureStockReader(userId: Long) {
        val user = userRepository.findById(userId)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "Пользователь не найден")
        if (user.status != UserStatus.ACTIVE) {
            throw ApiException(HttpStatusCode.Forbidden, "Доступ запрещён: учётная запись не активна")
        }
        RoleAccess.require(user.role, UserRole.ADMIN, UserRole.STOREKEEPER, UserRole.MANAGER)
    }

    private fun ensureStockOperator(userId: Long) {
        val user = userRepository.findById(userId)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "Пользователь не найден")
        if (user.status != UserStatus.ACTIVE) {
            throw ApiException(HttpStatusCode.Forbidden, "Доступ запрещён: учётная запись не активна")
        }
        RoleAccess.require(user.role, UserRole.ADMIN, UserRole.STOREKEEPER)
    }

    private fun validateActiveProduct(productId: Long) {
        val p = productRepository.findById(productId) ?: throw NotFoundException("Товар не найден")
        if (!p.isActive) throw ValidationException("Товар не активен")
    }

    private fun validateActiveWarehouse(warehouseId: Long) {
        val w = warehouseRepository.findById(warehouseId) ?: throw NotFoundException("Склад не найден")
        if (!w.isActive) throw ValidationException("Склад не активен")
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
        op: StockOperation,
        items: List<StockOperationItem>,
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
            items = items.map { item ->
                val p = productRepository.findById(item.productId)
                item.toItemResponse(p?.article, p?.name)
            },
        )
    }

    private fun responseAfterCreate(op: StockOperation): StockOperationResponse {
        val full = stockRepository.findOperationWithItems(op.id)
        requireNotNull(full)
        return enrichOperation(full.operation, full.items)
    }

    fun createReceipt(currentUserId: Long, request: CreateReceiptRequest): StockOperationResponse {
        ensureStockOperator(currentUserId)
        validateActiveProduct(request.productId)
        validateActiveWarehouse(request.warehouseId)
        val qty = stockOperationValidator.parseQuantity(request.quantity)
        val price = stockOperationValidator.parseMoney(request.price, "price")
        val op = stockRepository.createReceipt(
            warehouseId = request.warehouseId,
            productId = request.productId,
            quantity = qty,
            price = price,
            supplier = request.supplier,
            comment = request.comment,
            userId = currentUserId,
            now = dateTime.now(),
        )
        return responseAfterCreate(op)
    }

    private fun clipItemReason(reason: String?): String? {
        val t = reason?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (t.length <= STOCK_ITEM_REASON_MAX_LEN) t else t.substring(0, STOCK_ITEM_REASON_MAX_LEN)
    }

    fun createIssue(currentUserId: Long, request: CreateIssueRequest): StockOperationResponse {
        ensureStockOperator(currentUserId)
        validateActiveProduct(request.productId)
        validateActiveWarehouse(request.warehouseId)
        val qty = stockOperationValidator.parseQuantity(request.quantity)
        val op = stockRepository.createIssue(
            warehouseId = request.warehouseId,
            productId = request.productId,
            quantity = qty,
            reason = clipItemReason(request.reason),
            comment = request.comment,
            userId = currentUserId,
            now = dateTime.now(),
        )
        return responseAfterCreate(op)
    }

    fun createWriteOff(currentUserId: Long, request: CreateWriteOffRequest): StockOperationResponse {
        ensureStockOperator(currentUserId)
        validateActiveProduct(request.productId)
        validateActiveWarehouse(request.warehouseId)
        val qty = stockOperationValidator.parseQuantity(request.quantity)
        val op = stockRepository.createWriteOff(
            warehouseId = request.warehouseId,
            productId = request.productId,
            quantity = qty,
            reason = clipItemReason(request.reason),
            comment = request.comment,
            userId = currentUserId,
            now = dateTime.now(),
        )
        return responseAfterCreate(op)
    }

    fun createInventory(currentUserId: Long, request: CreateInventoryRequest): StockOperationResponse {
        ensureStockOperator(currentUserId)
        validateActiveProduct(request.productId)
        validateActiveWarehouse(request.warehouseId)
        val qty = stockOperationValidator.parseActualInventoryQuantity(request.actualQuantity)
        val op = stockRepository.createInventoryAdjustment(
            warehouseId = request.warehouseId,
            productId = request.productId,
            actualQuantity = qty,
            comment = request.comment,
            userId = currentUserId,
            now = dateTime.now(),
        )
        return responseAfterCreate(op)
    }
}
