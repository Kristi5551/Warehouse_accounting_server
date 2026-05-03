package com.example.warehouse_accounting_server.data.mapper

import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.reports.LowStockReport
import com.example.warehouse_accounting_server.domain.model.reports.OperationReport
import com.example.warehouse_accounting_server.domain.model.reports.StockValueItem
import com.example.warehouse_accounting_server.dto.response.report.LowStockReportResponse
import com.example.warehouse_accounting_server.dto.response.report.OperationReportItemResponse
import com.example.warehouse_accounting_server.dto.response.report.OperationReportResponse
import com.example.warehouse_accounting_server.dto.response.report.StockValueItemResponse

object ReportMapper {
    fun toResponse(r: LowStockReport): LowStockReportResponse =
        LowStockReportResponse(
            productId = r.productId,
            productArticle = r.productArticle,
            productName = r.productName,
            warehouseId = r.warehouseId,
            warehouseName = r.warehouseName,
            quantity = r.quantity.stripTrailingZeros().toPlainString(),
            minStock = r.minStock.stripTrailingZeros().toPlainString(),
        )

    fun toResponse(r: OperationReport): OperationReportResponse {
        val first = r.items.firstOrNull()
        val itemResponses =
            r.items.map { line ->
                OperationReportItemResponse(
                    productArticle = line.productArticle,
                    productName = line.productName,
                    quantity = line.quantity.stripTrailingZeros().toPlainString(),
                    price = line.price?.stripTrailingZeros()?.toPlainString(),
                )
            }
        return OperationReportResponse(
            operationId = r.operationId,
            operationType = r.operationType.name,
            warehouseId = r.warehouseId,
            warehouseName = r.warehouseName,
            createdByName = r.createdByName,
            createdAt = r.createdAt.toString(),
            productArticle = first?.productArticle ?: "",
            productName = first?.productName ?: "",
            quantity = first?.quantity?.stripTrailingZeros()?.toPlainString() ?: "0",
            price = first?.price?.stripTrailingZeros()?.toPlainString(),
            items = itemResponses,
        )
    }

    fun toResponse(r: StockValueItem): StockValueItemResponse =
        StockValueItemResponse(
            productId = r.productId,
            productArticle = r.productArticle,
            productName = r.productName,
            quantity = r.quantity.stripTrailingZeros().toPlainString(),
            purchasePrice = r.purchasePrice.stripTrailingZeros().toPlainString(),
            value = r.value.stripTrailingZeros().toPlainString(),
        )

    /** Одна запись [OperationReport] = одна операция (все строки уже сгруппированы по operationId). */
    fun distinctOpsByType(reports: List<OperationReport>, type: StockOperationType): Int =
        reports.count { it.operationType == type }
}
