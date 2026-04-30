package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.data.mapper.ReportMapper
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.repository.ReportRepository
import com.example.warehouse_accounting_server.dto.response.report.LowStockReportResponse
import com.example.warehouse_accounting_server.dto.response.report.OperationReportResponse
import com.example.warehouse_accounting_server.dto.response.report.StockSummaryReportResponse
import com.example.warehouse_accounting_server.dto.response.report.StockValueReportResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReportService(
    private val reportRepository: ReportRepository,
) {
    fun stockSummary(warehouseId: Long?): List<StockSummaryReportResponse> =
        reportRepository.stockSummary(warehouseId).map(ReportMapper::toResponse)

    fun lowStock(warehouseId: Long?): List<LowStockReportResponse> =
        reportRepository.lowStockReport(warehouseId).map(ReportMapper::toResponse)

    fun operations(
        operationType: StockOperationType?,
        productId: Long?,
        from: String?,
        to: String?,
        userId: Long?,
    ): List<OperationReportResponse> {
        val f = from?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        val t = to?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        return reportRepository.operationsReport(operationType, productId, f, t, userId).map(ReportMapper::toResponse)
    }

    fun stockValue(warehouseId: Long?): List<StockValueReportResponse> =
        reportRepository.stockValue(warehouseId).map(ReportMapper::toResponse)
}
