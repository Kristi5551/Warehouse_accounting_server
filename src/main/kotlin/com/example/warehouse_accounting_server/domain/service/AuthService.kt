package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ConflictException
import com.example.warehouse_accounting_server.config.ForbiddenException
import com.example.warehouse_accounting_server.config.NotFoundException
import com.example.warehouse_accounting_server.config.UnauthorizedException
import com.example.warehouse_accounting_server.data.mapper.toResponse
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.domain.validation.AuthValidator
import com.example.warehouse_accounting_server.dto.request.auth.LoginRequest
import com.example.warehouse_accounting_server.dto.request.auth.RegisterRequest
import com.example.warehouse_accounting_server.dto.response.auth.AuthResponse
import com.example.warehouse_accounting_server.dto.response.user.UserResponse
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.JwtProvider
import com.example.warehouse_accounting_server.util.PasswordHasher

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtProvider: JwtProvider,
    private val authValidator: AuthValidator,
    private val dateTime: DateTimeProvider,
) {
    fun register(request: RegisterRequest): UserResponse {
        authValidator.validateRegisterRequest(request)
        val email = request.email.trim().lowercase()
        if (userRepository.findByEmail(email) != null) {
            throw ConflictException("Пользователь с таким email уже существует")
        }
        val user = userRepository.create(
            email = email,
            passwordHash = passwordHasher.hash(request.password),
            fullName = request.fullName.trim(),
            role = request.requestedRole,
            status = UserStatus.PENDING,
            now = dateTime.now(),
        )
        return user.toResponse()
    }

    fun login(request: LoginRequest): AuthResponse {
        authValidator.validateLogin(request)
        val email = request.email.trim().lowercase()
        val user = userRepository.findByEmail(email)
            ?: throw UnauthorizedException("Неверный email или пароль")
        if (!passwordHasher.verify(request.password, user.passwordHash)) {
            throw UnauthorizedException("Неверный email или пароль")
        }
        if (user.status == UserStatus.PENDING) {
            throw ForbiddenException("Аккаунт ожидает подтверждения администратором")
        }
        if (user.status == UserStatus.BLOCKED) {
            throw ForbiddenException("Аккаунт заблокирован")
        }
        val token = jwtProvider.createAccessToken(user)
        return AuthResponse(
            token = token,
            user = user.toResponse(),
        )
    }

    fun getCurrentUser(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            ?: throw NotFoundException("Пользователь не найден")
        if (user.status != UserStatus.ACTIVE) {
            throw ForbiddenException("Пользователь больше не активен")
        }
        return user.toResponse()
    }
}
