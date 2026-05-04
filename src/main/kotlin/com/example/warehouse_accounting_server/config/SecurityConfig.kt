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

/** JWT здесь только идентифицирует пользователя (`userId` в payload). Права — в [AccessControlService] по БД. */
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
                    ErrorResponse(
                        message = "Токен авторизации отсутствует или недействителен",
                        details = null,
                    ),
                )
            }
        }
    }
}

fun JWTPrincipal.userId(): Long =
    payload.getClaim(JwtProvider.CLAIM_USER_ID).asLong()
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Недействительный токен")

/**
 * Роль из JWT (claim `role`) — **только справочно** для клиента; не использовать как источник прав на сервере.
 */
fun JWTPrincipal.userRole(): UserRole {
    val raw = payload.getClaim(JwtProvider.CLAIM_ROLE).asString()
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Недействительный токен")
    return runCatching { UserRole.valueOf(raw) }.getOrElse {
        throw ApiException(HttpStatusCode.Unauthorized, "Недействительный токен")
    }
}

/**
 * Проверка роли **по JWT-claim**, не по БД. Оставлено для редких сценариев; в маршрутах API
 * доступ по роли выполняется в сервисах через [com.example.warehouse_accounting_server.domain.service.AccessControlService].
 */
fun JWTPrincipal.requireRoles(vararg allowed: UserRole): UserRole {
    val role = userRole()
    RoleAccess.require(role, *allowed)
    return role
}
