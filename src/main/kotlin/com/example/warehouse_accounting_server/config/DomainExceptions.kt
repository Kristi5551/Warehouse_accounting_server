package com.example.warehouse_accounting_server.config

import io.ktor.http.HttpStatusCode

class ValidationException(
    message: String,
    details: String? = null,
) : ApiException(HttpStatusCode.BadRequest, message, details)

class UnauthorizedException(
    message: String,
    details: String? = null,
) : ApiException(HttpStatusCode.Unauthorized, message, details)

class ForbiddenException(
    message: String,
    details: String? = null,
) : ApiException(HttpStatusCode.Forbidden, message, details)

class NotFoundException(
    message: String,
    details: String? = null,
) : ApiException(HttpStatusCode.NotFound, message, details)

class ConflictException(
    message: String,
    details: String? = null,
) : ApiException(HttpStatusCode.Conflict, message, details)
