package com.example.warehouse_accounting_server.domain.model.reports

import com.example.warehouse_accounting_server.domain.model.StockOperationType
import java.time.LocalDateTime

/** Одна операция в отчёте; все строки [items] относятся к одному [operationId]. */
data class OperationReport(
    val operationId: Long,
    val operationType: StockOperationType,
    val warehouseId: Long,
    val warehouseName: String,
    val createdByName: String,
    val createdAt: LocalDateTime,
    val items: List<OperationReportItemLine>,
)
