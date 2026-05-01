package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.config.requireRoles
import com.example.warehouse_accounting_server.domain.model.UserRole
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
            // GET /api/products — ADMIN, STOREKEEPER, MANAGER
            get {
                call.principal<JWTPrincipal>()!!
                    .requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER, UserRole.MANAGER)
                val search = call.request.queryParameters["search"]?.trim()?.takeIf { it.isNotBlank() }
                val categoryId = call.request.queryParameters["categoryId"]?.toLongOrNull()
                val activeOnly = call.request.queryParameters["activeOnly"] != "false"
                call.respond(productService.list(search, categoryId, activeOnly))
            }

            // GET /api/products/{id} — ADMIN, STOREKEEPER, MANAGER
            get("/{id}") {
                call.principal<JWTPrincipal>()!!
                    .requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER, UserRole.MANAGER)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ApiException(HttpStatusCode.BadRequest, "Неверный идентификатор")
                call.respond(productService.getById(id))
            }

            // POST /api/products — только ADMIN
            post {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                val body = call.receive<CreateProductRequest>()
                call.respond(HttpStatusCode.Created, productService.create(body))
            }

            // PUT /api/products/{id} — только ADMIN
            put("/{id}") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ApiException(HttpStatusCode.BadRequest, "Неверный идентификатор")
                val body = call.receive<UpdateProductRequest>()
                call.respond(productService.update(id, body))
            }

            // DELETE /api/products/{id} — мягкое удаление, только ADMIN
            delete("/{id}") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ApiException(HttpStatusCode.BadRequest, "Неверный идентификатор")
                call.respond(productService.deactivate(id))
            }
        }
    }
}
