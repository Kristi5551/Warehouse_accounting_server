package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.reports.LowStockReport
import com.example.warehouse_accounting_server.domain.model.reports.OperationReport
import com.example.warehouse_accounting_server.domain.model.reports.StockValueItem
import java.time.LocalDate

interface ReportRepository {
    fun lowStockReport(warehouseId: Long? = null): List<LowStockReport>
    fun operationsReport(dateFrom: LocalDate?, dateTo: LocalDate?): List<OperationReport>
    fun stockValueLines(warehouseId: Long? = null): List<StockValueItem>
}
