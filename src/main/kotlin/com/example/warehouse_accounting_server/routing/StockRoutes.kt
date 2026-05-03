package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.requireRoles
import com.example.warehouse_accounting_server.config.userId
import com.example.warehouse_accounting_server.config.userRole
import com.example.warehouse_accounting_server.domain.model.StockStatus
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
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val search = call.request.queryParameters["search"]
                val categoryId = call.request.queryParameters["categoryId"]?.toLongOrNull()
                val status = call.request.queryParameters["status"]?.let { StockStatus.valueOf(it) }
                call.respond(stockService.getBalances(userId, search, categoryId, status))
            }
            get("/low") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.MANAGER)
                val userId = call.principal<JWTPrincipal>()!!.userId()
                call.respond(stockService.getLowStock(userId))
            }
            get("/products/{id}/history") {
                val principalUserId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]!!.toLong()
                val type =
                    parseOperationTypeQuery(call.request.queryParameters["type"])
                        ?: parseOperationTypeQuery(call.request.queryParameters["operationType"])
                val filterUserId = call.request.queryParameters["userId"]?.toLongOrNull()
                val dateFrom = parseDateQuery(call.request.queryParameters["dateFrom"])
                val dateTo = parseDateQuery(call.request.queryParameters["dateTo"])
                call.respond(
                    stockService.getProductHistory(principalUserId, id, type, filterUserId, dateFrom, dateTo),
                )
            }
            post("/receipt") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val body = call.receive<CreateReceiptRequest>()
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val created = stockService.createReceipt(userId, body)
                call.respond(HttpStatusCode.Created, created)
            }
            post("/issue") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val body = call.receive<CreateIssueRequest>()
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val created = stockService.createIssue(userId, body)
                call.respond(HttpStatusCode.Created, created)
            }
            post("/write-off") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val body = call.receive<CreateWriteOffRequest>()
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val created = stockService.createWriteOff(userId, body)
                call.respond(HttpStatusCode.Created, created)
            }
            post("/inventory") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val body = call.receive<CreateInventoryRequest>()
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val created = stockService.createInventory(userId, body)
                call.respond(HttpStatusCode.Created, created)
            }
        }
    }
}
