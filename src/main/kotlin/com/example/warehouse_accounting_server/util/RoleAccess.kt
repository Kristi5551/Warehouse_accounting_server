package com.example.warehouse_accounting_server.util

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.domain.model.UserRole
import io.ktor.http.HttpStatusCode

object RoleAccess {
    fun require(actual: UserRole, vararg allowed: UserRole) {
        if (actual !in allowed) {
            throw ApiException(HttpStatusCode.Forbidden, "Недостаточно прав")
        }
    }
}
