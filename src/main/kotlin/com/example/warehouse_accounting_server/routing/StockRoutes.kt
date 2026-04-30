package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.requireRoles
import com.example.warehouse_accounting_server.config.userId
import com.example.warehouse_accounting_server.config.userRole
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.service.StockService
import com.example.warehouse_accounting_server.dto.request.stock.CreateInventoryRequest
import com.example.warehouse_accounting_server.dto.request.stock.CreateIssueRequest
import com.example.warehouse_accounting_server.dto.request.stock.CreateReceiptRequest
import com.example.warehouse_accounting_server.dto.request.stock.CreateWriteOffRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.stockRoutes(stockService: StockService) {
    authenticate("auth-jwt") {
        route("/api/stock") {
            get("/balances") {
                val wh = call.request.queryParameters["warehouseId"]?.toLongOrNull()
                call.respond(stockService.balances(wh))
            }
            get("/low") {
                val wh = call.request.queryParameters["warehouseId"]?.toLongOrNull()
                call.respond(stockService.lowStock(wh))
            }
            get("/products/{id}/history") {
                val id = call.parameters["id"]!!.toLong()
                val type = call.request.queryParameters["operationType"]?.let { StockOperationType.valueOf(it) }
                val from = call.request.queryParameters["from"]
                val to = call.request.queryParameters["to"]
                val userId = call.request.queryParameters["userId"]?.toLongOrNull()
                call.respond(stockService.productHistory(id, type, from, to, userId))
            }
            post("/receipt") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val body = call.receive<CreateReceiptRequest>()
                stockService.receipt(body, call.principal<JWTPrincipal>()!!.userId())
                call.respond(HttpStatusCode.Created)
            }
            post("/issue") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val body = call.receive<CreateIssueRequest>()
                stockService.issue(body, call.principal<JWTPrincipal>()!!.userId())
                call.respond(HttpStatusCode.Created)
            }
            post("/write-off") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val body = call.receive<CreateWriteOffRequest>()
                stockService.writeOff(body, call.principal<JWTPrincipal>()!!.userId())
                call.respond(HttpStatusCode.Created)
            }
            post("/inventory") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val body = call.receive<CreateInventoryRequest>()
                stockService.inventory(body, call.principal<JWTPrincipal>()!!.userId())
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}
