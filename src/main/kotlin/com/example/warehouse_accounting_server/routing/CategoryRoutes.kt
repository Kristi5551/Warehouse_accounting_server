package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.requireRoles
import com.example.warehouse_accounting_server.config.userId
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
            // GET /api/categories — ADMIN, STOREKEEPER, MANAGER
            get {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                call.principal<JWTPrincipal>()!!
                    .requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER, UserRole.MANAGER)
                val activeOnly = call.request.queryParameters["activeOnly"] != "false"
                call.respond(categoryService.list(actorId, activeOnly))
            }

            // GET /api/categories/{id} — ADMIN, STOREKEEPER, MANAGER
            get("/{id}") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                call.principal<JWTPrincipal>()!!
                    .requireRoles(UserRole.ADMIN, UserRole.STOREKEEPER, UserRole.MANAGER)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw com.example.warehouse_accounting_server.config.ApiException(
                        io.ktor.http.HttpStatusCode.BadRequest, "Неверный идентификатор",
                    )
                call.respond(categoryService.getById(actorId, id))
            }

            // POST /api/categories — только ADMIN
            post {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                val body = call.receive<CreateCategoryRequest>()
                call.respond(HttpStatusCode.Created, categoryService.create(actorId, body))
            }

            // PUT /api/categories/{id} — только ADMIN
            put("/{id}") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw com.example.warehouse_accounting_server.config.ApiException(
                        io.ktor.http.HttpStatusCode.BadRequest, "Неверный идентификатор",
                    )
                val body = call.receive<UpdateCategoryRequest>()
                call.respond(categoryService.update(actorId, id, body))
            }

            // DELETE /api/categories/{id} — мягкое удаление, только ADMIN
            delete("/{id}") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw com.example.warehouse_accounting_server.config.ApiException(
                        io.ktor.http.HttpStatusCode.BadRequest, "Неверный идентификатор",
                    )
                call.respond(categoryService.deactivate(actorId, id))
            }
        }
    }
}
