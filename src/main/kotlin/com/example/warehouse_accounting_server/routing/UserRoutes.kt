package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.domain.service.UserService
import com.example.warehouse_accounting_server.dto.request.user.ApproveUserRequest
import com.example.warehouse_accounting_server.dto.request.user.ChangeUserRoleRequest
import com.example.warehouse_accounting_server.dto.request.user.CreateAdminUserRequest
import com.example.warehouse_accounting_server.config.userId
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
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.userRoutes(userService: UserService) {
    authenticate("auth-jwt") {
        route("/api/users") {
            post("/admin") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val body = call.receive<CreateAdminUserRequest>()
                call.respond(HttpStatusCode.Created, userService.createAdminUser(actorId, body))
            }
            get {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                call.respond(userService.getAllUsers(actorId))
            }
            get("/for-operation-filters") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                call.respond(userService.listUsersForOperationFilters(actorId))
            }
            get("/pending") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                call.respond(userService.getPendingUsers(actorId))
            }
            patch("/{id}/approve") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]!!.toLong()
                runCatching { call.receive<ApproveUserRequest>() }.getOrElse { ApproveUserRequest() }
                call.respond(userService.approveUser(actorId, id))
            }
            patch("/{id}/block") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]!!.toLong()
                call.respond(userService.blockUser(actorId, id))
            }
            patch("/{id}/unblock") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]!!.toLong()
                call.respond(userService.unblockUser(actorId, id))
            }
            patch("/{id}/role") {
                val actorId = call.principal<JWTPrincipal>()!!.userId()
                val id = call.parameters["id"]!!.toLong()
                val body = call.receive<ChangeUserRoleRequest>()
                call.respond(userService.changeUserRole(actorId, id, body))
            }
        }
    }
}
