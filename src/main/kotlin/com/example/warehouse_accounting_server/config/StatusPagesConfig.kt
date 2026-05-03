package com.example.warehouse_accounting_server.config

import com.example.warehouse_accounting_server.dto.response.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(
                cause.statusCode,
                ErrorResponse(message = cause.message, details = cause.details),
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(message = cause.message ?: "Неверный запрос", details = null),
            )
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(message = "Внутренняя ошибка сервера", details = null),
            )
        }
    }
}
