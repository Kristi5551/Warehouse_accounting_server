package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.service.ReportService
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.reportRoutes(reportService: ReportService) {
    authenticate("auth-jwt") {
        route("/api/reports") {
            get("/stock-summary") {
                val wh = call.request.queryParameters["warehouseId"]?.toLongOrNull()
                call.respond(reportService.stockSummary(wh))
            }
            get("/low-stock") {
                val wh = call.request.queryParameters["warehouseId"]?.toLongOrNull()
                call.respond(reportService.lowStock(wh))
            }
            get("/operations") {
                val type = call.request.queryParameters["operationType"]?.let { StockOperationType.valueOf(it) }
                val productId = call.request.queryParameters["productId"]?.toLongOrNull()
                val from = call.request.queryParameters["from"]
                val to = call.request.queryParameters["to"]
                val userId = call.request.queryParameters["userId"]?.toLongOrNull()
                call.respond(reportService.operations(type, productId, from, to, userId))
            }
            get("/stock-value") {
                val wh = call.request.queryParameters["warehouseId"]?.toLongOrNull()
                call.respond(reportService.stockValue(wh))
            }
        }
    }
}
