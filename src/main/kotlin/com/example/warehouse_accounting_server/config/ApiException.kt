package com.example.warehouse_accounting_server.config

import io.ktor.http.HttpStatusCode

class ApiException(
    val statusCode: HttpStatusCode,
    override val message: String,
    val details: String? = null,
) : RuntimeException(message)
