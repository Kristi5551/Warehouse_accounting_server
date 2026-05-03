package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.data.mapper.ReportMapper
import com.example.warehouse_accounting_server.data.mapper.toBalanceResponse
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.ReportRepository
import com.example.warehouse_accounting_server.domain.repository.StockRepository
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.domain.model.StockStatus
import com.example.warehouse_accounting_server.domain.model.stockStatusFor
import com.example.warehouse_accounting_server.dto.response.report.LowStockReportResponse
import com.example.warehouse_accounting_server.dto.response.report.OperationsReportBundleResponse
import com.example.warehouse_accounting_server.dto.response.report.StockSummaryReportResponse
import com.example.warehouse_accounting_server.dto.response.report.StockValueReportResponse
import com.example.warehouse_accounting_server.util.RoleAccess
import io.ktor.http.HttpStatusCode
import java.math.BigDecimal
import java.time.LocalDate

class ReportService(
    private val reportRepository: ReportRepository,
    private val stockRepository: StockRepository,
    private val userRepository: UserRepository,
) {
    private fun ensureReportReader(userId: Long) {
        val user =
            userRepository.findById(userId)
                ?: throw ApiException(HttpStatusCode.Unauthorized, "Пользователь не найден")
        if (user.status != UserStatus.ACTIVE) {
            throw ApiException(HttpStatusCode.Forbidden, "Доступ запрещён: учётная запись не активна")
        }
        RoleAccess.require(user.role, UserRole.ADMIN, UserRole.MANAGER)
    }

    fun stockSummary(actorId: Long, warehouseId: Long?): StockSummaryReportResponse {
        ensureReportReader(actorId)
        val views =
            stockRepository.getBalances(search = null, categoryId = null, status = null)
                .filter { warehouseId == null || it.warehouseId == warehouseId }
        var inStock = 0
        var low = 0
        var out = 0
        for (v in views) {
            when (stockStatusFor(v.quantity, v.minStock)) {
                StockStatus.IN_STOCK -> inStock++
                StockStatus.LOW_STOCK -> low++
                StockStatus.OUT_OF_STOCK -> out++
            }
        }
        val balances = views.map { it.toBalanceResponse() }
        return StockSummaryReportResponse(
            totalProducts = views.size,
            inStockCount = inStock,
            lowStockCount = low,
            outOfStockCount = out,
            balances = balances,
        )
    }

    fun lowStock(actorId: Long, warehouseId: Long?): List<LowStockReportResponse> {
        ensureReportReader(actorId)
        return reportRepository.lowStockReport(warehouseId).map(ReportMapper::toResponse)
    }

    fun operations(actorId: Long, dateFrom: LocalDate?, dateTo: LocalDate?): OperationsReportBundleResponse {
        ensureReportReader(actorId)
        val lines = reportRepository.operationsReport(dateFrom, dateTo)
        return OperationsReportBundleResponse(
            operations = lines.map(ReportMapper::toResponse),
            receiptCount = ReportMapper.distinctOpsByType(lines, StockOperationType.RECEIPT),
            issueCount = ReportMapper.distinctOpsByType(lines, StockOperationType.ISSUE),
            writeOffCount = ReportMapper.distinctOpsByType(lines, StockOperationType.WRITE_OFF),
            inventoryCount = ReportMapper.distinctOpsByType(lines, StockOperationType.INVENTORY),
        )
    }

    fun stockValue(actorId: Long, warehouseId: Long?): StockValueReportResponse {
        ensureReportReader(actorId)
        val items = reportRepository.stockValueLines(warehouseId)
        val total = items.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.value) }
        return StockValueReportResponse(
            totalValue = total.stripTrailingZeros().toPlainString(),
            items = items.map(ReportMapper::toResponse),
        )
    }
}
