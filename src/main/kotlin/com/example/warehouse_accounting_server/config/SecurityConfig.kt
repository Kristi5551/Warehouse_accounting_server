package com.example.warehouse_accounting_server.config

import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.dto.response.ErrorResponse
import com.example.warehouse_accounting_server.util.JwtProvider
import com.example.warehouse_accounting_server.util.RoleAccess
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

fun Application.configureSecurity(
    appConfig: AppConfig,
    jwtProvider: JwtProvider,
) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = appConfig.jwt.realm
            verifier(jwtProvider.buildVerifier())
            validate { credential ->
                val userId = credential.payload.getClaim(JwtProvider.CLAIM_USER_ID).asLong()
                if (userId == null) {
                    return@validate null
                }
                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(message = "Unauthorized", details = null),
                )
            }
        }
    }
}

fun JWTPrincipal.userId(): Long =
    payload.getClaim(JwtProvider.CLAIM_USER_ID).asLong()
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Invalid token")

fun JWTPrincipal.userRole(): UserRole {
    val raw = payload.getClaim(JwtProvider.CLAIM_ROLE).asString()
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Invalid token")
    return runCatching { UserRole.valueOf(raw) }.getOrElse {
        throw ApiException(HttpStatusCode.Unauthorized, "Invalid token")
    }
}

fun JWTPrincipal.requireRoles(vararg allowed: UserRole): UserRole {
    val role = userRole()
    RoleAccess.require(role, *allowed)
    return role
}
