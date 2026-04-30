package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.requireRoles
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.service.UserService
import com.example.warehouse_accounting_server.dto.request.user.ApproveUserRequest
import com.example.warehouse_accounting_server.dto.request.user.ChangeUserRoleRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route

fun Route.userRoutes(userService: UserService) {
    authenticate("auth-jwt") {
        route("/api/users") {
            get {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                call.respond(userService.list())
            }
            get("/pending") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                call.respond(userService.listPending())
            }
            patch("/{id}/approve") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                val id = call.parameters["id"]!!.toLong()
                val body = call.receive<ApproveUserRequest>()
                userService.approve(id, body)
                call.respond(HttpStatusCode.NoContent)
            }
            patch("/{id}/block") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                val id = call.parameters["id"]!!.toLong()
                userService.block(id)
                call.respond(HttpStatusCode.NoContent)
            }
            patch("/{id}/unblock") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                val id = call.parameters["id"]!!.toLong()
                userService.unblock(id)
                call.respond(HttpStatusCode.NoContent)
            }
            patch("/{id}/role") {
                call.principal<JWTPrincipal>()!!.requireRoles(UserRole.ADMIN)
                val id = call.parameters["id"]!!.toLong()
                val body = call.receive<ChangeUserRoleRequest>()
                userService.changeRole(id, body)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
