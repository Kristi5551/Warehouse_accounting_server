package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.userId
import com.example.warehouse_accounting_server.domain.service.ReportService
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Отчёты под `/api/reports`.
 *
 * **Отчёт по низким остаткам:** `GET /api/reports/low-stock` — строки отчёта
 * ([com.example.warehouse_accounting_server.dto.response.report.LowStockReportResponse]) для аналитики;
 * опционально `warehouseId`. Доступ: `requireReportReader` (ADMIN, MANAGER).
 * Операционный просмотр без фильтра отчёта — `GET /api/stock/low`.
 */
fun Route.reportRoutes(reportService: ReportService) {
    authenticate("auth-jwt") {
        route("/api/reports") {
            get("/stock-summary") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val wh = call.request.queryParameters["warehouseId"]?.toLongOrNull()
                call.respond(reportService.stockSummary(actorId, wh))
            }
            /** Low-stock report rows / analytics (optional warehouse filter). */
            get("/low-stock") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val wh = call.request.queryParameters["warehouseId"]?.toLongOrNull()
                call.respond(reportService.lowStock(actorId, wh))
            }
            /**
             * Отчёт по операциям за период. Query: [parseDateQuery] для **dateFrom** / **dateTo** (`yyyy-MM-dd`);
             * см. [com.example.warehouse_accounting_server.util.ReportDateBounds].
             */
            get("/operations") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val dateFrom = parseDateQuery(call.request.queryParameters["dateFrom"])
                val dateTo = parseDateQuery(call.request.queryParameters["dateTo"])
                call.respond(reportService.operations(actorId, dateFrom, dateTo))
            }
            get("/stock-value") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val wh = call.request.queryParameters["warehouseId"]?.toLongOrNull()
                call.respond(reportService.stockValue(actorId, wh))
            }
        }
    }
}
