package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.domain.service.AuthService
import com.example.warehouse_accounting_server.dto.request.auth.LoginRequest
import com.example.warehouse_accounting_server.dto.request.auth.RegisterRequest
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
import com.example.warehouse_accounting_server.config.userId

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/register") {
            val body = call.receive<RegisterRequest>()
            val res = authService.register(body)
            call.respond(HttpStatusCode.Created, res)
        }
        post("/login") {
            val body = call.receive<LoginRequest>()
            call.respond(authService.login(body))
        }
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                call.respond(authService.me(principal.userId()))
            }
        }
    }
}
