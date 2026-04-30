package com.example.warehouse_accounting_server.domain.validation

import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.dto.request.auth.RegisterRequest

class AuthValidator {
    fun validateRegister(request: RegisterRequest) {
        require(request.fullName.isNotBlank()) { "fullName required" }
        require(request.email.isNotBlank()) { "email required" }
        require(request.password.length >= 6) { "password too short" }
        require(
            runCatching { UserRole.valueOf(request.requestedRole) }.isSuccess,
        ) { "invalid requestedRole" }
    }
}
