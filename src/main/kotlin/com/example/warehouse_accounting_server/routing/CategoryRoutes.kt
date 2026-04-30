package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.requireRoles
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.service.CategoryService
import com.example.warehouse_accounting_server.dto.request.category.CreateCategoryRequest
import com.example.warehouse_accounting_server.dto.request.category.UpdateCategoryRequest
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

fun Route.categoryRoutes(categoryService: CategoryService) {
    authenticate("auth-jwt") {
        route("/api/categories") {
            get {
                call.respond(categoryService.list())
            }
            post {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val body = call.receive<CreateCategoryRequest>()
                call.respond(HttpStatusCode.Created, categoryService.create(body))
            }
            put("/{id}") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val id = call.parameters["id"]!!.toLong()
                val body = call.receive<UpdateCategoryRequest>()
                call.respond(categoryService.update(id, body))
            }
            delete("/{id}") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER)
                val id = call.parameters["id"]!!.toLong()
                categoryService.deactivate(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
