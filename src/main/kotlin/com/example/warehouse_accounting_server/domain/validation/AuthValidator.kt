package com.example.warehouse_accounting_server.domain.validation

import com.example.warehouse_accounting_server.config.ValidationException
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.dto.request.auth.LoginRequest
import com.example.warehouse_accounting_server.dto.request.auth.RegisterRequest

class AuthValidator {
    private val emailRegex = Regex("^[A-Za-z0-9+._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    private val selfRegistrationRoles = setOf(UserRole.STOREKEEPER, UserRole.MANAGER)

    fun validateRegister(request: RegisterRequest) {
        validateRegisterRequest(request)
    }

    /**
     * Самостоятельная регистрация: только [UserRole.STOREKEEPER] или [UserRole.MANAGER].
     * Роль [UserRole.ADMIN] создаётся только действующим администратором (например POST /api/users/admin).
     */
    fun validateRegisterRequest(request: RegisterRequest) {
        val fullName = request.fullName.trim()
        if (fullName.isEmpty()) {
            throw ValidationException("Укажите ФИО")
        }
        val email = request.email.trim()
        if (email.isEmpty()) {
            throw ValidationException("Введите email")
        }
        if (!email.matches(emailRegex)) {
            throw ValidationException("Некорректный формат email")
        }
        if (request.password.isEmpty()) {
            throw ValidationException("Введите пароль")
        }
        if (request.password.length < 6) {
            throw ValidationException("Пароль должен содержать минимум 6 символов")
        }
        if (request.requestedRole !in selfRegistrationRoles) {
            throw ValidationException("Нельзя зарегистрироваться с ролью администратора")
        }
    }

    fun validateLogin(request: LoginRequest) {
        if (request.email.isBlank()) {
            throw ValidationException("Введите email")
        }
        if (request.password.isEmpty()) {
            throw ValidationException("Введите пароль")
        }
    }
}
