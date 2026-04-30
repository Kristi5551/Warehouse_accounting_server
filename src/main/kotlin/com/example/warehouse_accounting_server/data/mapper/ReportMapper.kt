package com.example.warehouse_accounting_server.data.mapper

import com.example.warehouse_accounting_server.domain.model.reports.LowStockReport
import com.example.warehouse_accounting_server.domain.model.reports.OperationReport
import com.example.warehouse_accounting_server.domain.model.reports.StockSummaryReport
import com.example.warehouse_accounting_server.domain.model.reports.StockValueReport
import com.example.warehouse_accounting_server.dto.response.report.LowStockReportResponse
import com.example.warehouse_accounting_server.dto.response.report.OperationReportResponse
import com.example.warehouse_accounting_server.dto.response.report.StockSummaryReportResponse
import com.example.warehouse_accounting_server.dto.response.report.StockValueReportResponse

object ReportMapper {
    fun toResponse(r: StockSummaryReport): StockSummaryReportResponse =
        StockSummaryReportResponse(
            warehouseId = r.warehouseId,
            warehouseName = r.warehouseName,
            productId = r.productId,
            productArticle = r.productArticle,
            productName = r.productName,
            quantity = r.quantity.stripTrailingZeros().toPlainString(),
            unit = r.unit,
        )

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

    fun toResponse(r: OperationReport): OperationReportResponse =
        OperationReportResponse(
            operationId = r.operationId,
            operationType = r.operationType.name,
            warehouseId = r.warehouseId,
            warehouseName = r.warehouseName,
            createdByName = r.createdByName,
            createdAt = r.createdAt.toString(),
            productArticle = r.productArticle,
            productName = r.productName,
            quantity = r.quantity.stripTrailingZeros().toPlainString(),
            price = r.price?.stripTrailingZeros()?.toPlainString(),
        )

    fun toResponse(r: StockValueReport): StockValueReportResponse =
        StockValueReportResponse(
            warehouseId = r.warehouseId,
            warehouseName = r.warehouseName,
            totalPurchaseValue = r.totalPurchaseValue.stripTrailingZeros().toPlainString(),
            totalSaleValue = r.totalSaleValue.stripTrailingZeros().toPlainString(),
        )
}
