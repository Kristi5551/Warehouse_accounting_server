package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.config.userId
import com.example.warehouse_accounting_server.domain.service.ProductService
import com.example.warehouse_accounting_server.dto.request.product.CreateProductRequest
import com.example.warehouse_accounting_server.dto.request.product.UpdateProductRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.productRoutes(productService: ProductService) {
    authenticate("auth-jwt") {
        route("/api/products") {
            get {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val search = call.request.queryParameters["search"]?.trim()?.takeIf { it.isNotBlank() }
                val categoryId = call.request.queryParameters["categoryId"]?.toLongOrNull()
                val activeOnly = call.request.queryParameters["activeOnly"] != "false"
                call.respond(productService.list(actorId, search, categoryId, activeOnly))
            }

            get("/{id}") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ApiException(HttpStatusCode.BadRequest, "Неверный идентификатор")
                call.respond(productService.getById(actorId, id))
            }

            post {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val body = call.receive<CreateProductRequest>()
                call.respond(HttpStatusCode.Created, productService.create(actorId, body))
            }

            put("/{id}") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ApiException(HttpStatusCode.BadRequest, "Неверный идентификатор")
                val body = call.receive<UpdateProductRequest>()
                call.respond(productService.update(actorId, id, body))
            }

            delete("/{id}") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ApiException(HttpStatusCode.BadRequest, "Неверный идентификатор")
                call.respond(productService.deactivate(actorId, id))
            }
        }
    }
}
