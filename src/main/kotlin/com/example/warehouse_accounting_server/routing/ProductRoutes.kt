package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.requireRoles
import com.example.warehouse_accounting_server.config.userRole
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
            get {
                val categoryId = call.request.queryParameters["categoryId"]?.toLongOrNull()
                val search = call.request.queryParameters["search"]
                val list = if (call.principal<JWTPrincipal>()!!.userRole() == UserRole.MANAGER) {
                    productService.list(categoryId, search)
                } else {
                    productService.adminList(categoryId, search)
                }
                call.respond(list)
            }
            get("/{id}") {
                val id = call.parameters["id"]!!.toLong()
                call.respond(productService.getById(id))
            }
            post {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val body = call.receive<CreateProductRequest>()
                call.respond(HttpStatusCode.Created, productService.create(body))
            }
            put("/{id}") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val id = call.parameters["id"]!!.toLong()
                val body = call.receive<UpdateProductRequest>()
                call.respond(productService.update(id, body))
            }
            delete("/{id}") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val id = call.parameters["id"]!!.toLong()
                productService.deactivate(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
