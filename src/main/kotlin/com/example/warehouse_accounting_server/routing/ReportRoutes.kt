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

fun Route.reportRoutes(reportService: ReportService) {
    authenticate("auth-jwt") {
        route("/api/reports") {
            get("/stock-summary") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val wh = call.request.queryParameters["warehouseId"]?.toLongOrNull()
                call.respond(reportService.stockSummary(actorId, wh))
            }
            get("/low-stock") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val wh = call.request.queryParameters["warehouseId"]?.toLongOrNull()
                call.respond(reportService.lowStock(actorId, wh))
            }
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
