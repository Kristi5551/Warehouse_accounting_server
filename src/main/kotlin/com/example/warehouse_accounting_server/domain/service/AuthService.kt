package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.data.mapper.toResponse
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.domain.validation.AuthValidator
import com.example.warehouse_accounting_server.dto.request.auth.LoginRequest
import com.example.warehouse_accounting_server.dto.request.auth.RegisterRequest
import com.example.warehouse_accounting_server.dto.response.auth.AuthResponse
import com.example.warehouse_accounting_server.dto.response.auth.CurrentUserResponse
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.JwtProvider
import com.example.warehouse_accounting_server.util.PasswordHasher
import io.ktor.http.HttpStatusCode

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtProvider: JwtProvider,
    private val authValidator: AuthValidator,
    private val dateTime: DateTimeProvider,
) {
    fun register(request: RegisterRequest): AuthResponse {
        authValidator.validateRegister(request)
        val email = request.email.trim().lowercase()
        if (userRepository.findByEmail(email) != null) {
            throw ApiException(HttpStatusCode.Conflict, "Email already registered")
        }
        val role = UserRole.valueOf(request.requestedRole)
        val user = userRepository.create(
            email = email,
            passwordHash = passwordHasher.hash(request.password),
            fullName = request.fullName.trim(),
            role = role,
            status = UserStatus.PENDING,
            now = dateTime.now(),
        )
        val token = jwtProvider.createAccessToken(user.id, user.role)
        return AuthResponse(
            token = token,
            user = user.toResponse(),
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        val user = userRepository.findByEmail(email)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "Invalid credentials")
        if (!passwordHasher.verify(request.password, user.passwordHash)) {
            throw ApiException(HttpStatusCode.Unauthorized, "Invalid credentials")
        }
        if (user.status == UserStatus.BLOCKED) {
            throw ApiException(HttpStatusCode.Forbidden, "User blocked")
        }
        if (user.status == UserStatus.PENDING) {
            throw ApiException(HttpStatusCode.Forbidden, "User not approved yet")
        }
        val token = jwtProvider.createAccessToken(user.id, user.role)
        return AuthResponse(
            token = token,
            user = user.toResponse(),
        )
    }

    fun me(userId: Long): CurrentUserResponse {
        val user = userRepository.findById(userId)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "User not found")
        return CurrentUserResponse.from(user.toResponse())
    }
}
