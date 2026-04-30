package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.reports.LowStockReport
import com.example.warehouse_accounting_server.domain.model.reports.OperationReport
import com.example.warehouse_accounting_server.domain.model.reports.StockSummaryReport
import com.example.warehouse_accounting_server.domain.model.reports.StockValueReport
import java.time.LocalDateTime

interface ReportRepository {
    fun stockSummary(warehouseId: Long? = null): List<StockSummaryReport>
    fun lowStockReport(warehouseId: Long? = null): List<LowStockReport>
    fun operationsReport(
        operationType: com.example.warehouse_accounting_server.domain.model.StockOperationType? = null,
        productId: Long? = null,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        userId: Long? = null,
    ): List<OperationReport>

    fun stockValue(warehouseId: Long? = null): List<StockValueReport>
}
