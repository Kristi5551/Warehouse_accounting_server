package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.userId
import com.example.warehouse_accounting_server.domain.service.StockService
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.operationRoutes(stockService: StockService) {
    authenticate("auth-jwt") {
        get("/api/operations") {
            val userId = call.principal<JWTPrincipal>()!!.userId()
            val type = parseOperationTypeQuery(call.request.queryParameters["type"])
            val productId = call.request.queryParameters["productId"]?.toLongOrNull()
            val filterUserId = call.request.queryParameters["userId"]?.toLongOrNull()
            val dateFrom = parseDateQuery(call.request.queryParameters["dateFrom"])
            val dateTo = parseDateQuery(call.request.queryParameters["dateTo"])
            call.respond(
                stockService.getOperations(userId, type, productId, filterUserId, dateFrom, dateTo),
            )
        }
    }
}
