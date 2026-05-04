package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.userId
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
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val activeOnly = call.request.queryParameters["activeOnly"] != "false"
                call.respond(categoryService.list(actorId, activeOnly))
            }

            get("/{id}") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw com.example.warehouse_accounting_server.config.ApiException(
                        io.ktor.http.HttpStatusCode.BadRequest, "Неверный идентификатор",
                    )
                call.respond(categoryService.getById(actorId, id))
            }

            post {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val body = call.receive<CreateCategoryRequest>()
                call.respond(HttpStatusCode.Created, categoryService.create(actorId, body))
            }

            put("/{id}") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw com.example.warehouse_accounting_server.config.ApiException(
                        io.ktor.http.HttpStatusCode.BadRequest, "Неверный идентификатор",
                    )
                val body = call.receive<UpdateCategoryRequest>()
                call.respond(categoryService.update(actorId, id, body))
            }

            delete("/{id}") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw com.example.warehouse_accounting_server.config.ApiException(
                        io.ktor.http.HttpStatusCode.BadRequest, "Неверный идентификатор",
                    )
                call.respond(categoryService.deactivate(actorId, id))
            }
        }
    }
}
