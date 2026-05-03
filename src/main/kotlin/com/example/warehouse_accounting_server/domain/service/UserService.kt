package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ConflictException
import com.example.warehouse_accounting_server.config.ForbiddenException
import com.example.warehouse_accounting_server.config.NotFoundException
import com.example.warehouse_accounting_server.config.ValidationException
import com.example.warehouse_accounting_server.data.mapper.toResponse
import com.example.warehouse_accounting_server.domain.model.User
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.domain.validation.AuthValidator
import com.example.warehouse_accounting_server.dto.request.auth.RegisterRequest
import com.example.warehouse_accounting_server.dto.request.user.ChangeUserRoleRequest
import com.example.warehouse_accounting_server.dto.request.user.CreateAdminUserRequest
import com.example.warehouse_accounting_server.dto.response.user.UserBriefResponse
import com.example.warehouse_accounting_server.dto.response.user.UserResponse
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.PasswordHasher

class UserService(
    private val userRepository: UserRepository,
    private val dateTime: DateTimeProvider,
    private val passwordHasher: PasswordHasher,
    private val authValidator: AuthValidator,
    private val accessControl: AccessControlService,
) {
    private fun findTargetOrThrow(id: Long): User =
        userRepository.findById(id) ?: throw NotFoundException("Пользователь не найден")

    private fun ensureNotLastActiveAdminBlock(target: User) {
        val onlyOne =
            target.role == UserRole.ADMIN &&
                target.status == UserStatus.ACTIVE &&
                userRepository.countActiveAdmins() == 1L
        if (onlyOne) {
            throw ForbiddenException("Нельзя заблокировать последнего администратора системы")
        }
    }

    private fun ensureNotLastActiveAdminDemote(target: User, newRole: UserRole) {
        if (newRole == UserRole.ADMIN) return
        val onlyOne =
            target.role == UserRole.ADMIN &&
                target.status == UserStatus.ACTIVE &&
                userRepository.countActiveAdmins() == 1L
        if (onlyOne) {
            throw ForbiddenException(
                "Нельзя снять роль администратора с последней активной учётной записи администратора",
            )
        }
    }

    fun getAllUsers(actorId: Long): List<UserResponse> {
        accessControl.requireActiveAdmin(actorId)
        return userRepository.listAll().map { it.toResponse() }
    }

    fun listUsersForOperationFilters(actorId: Long): List<UserBriefResponse> {
        accessControl.requireStockReader(actorId)
        return userRepository.listAll()
            .asSequence()
            .filter { it.status == UserStatus.ACTIVE }
            .sortedBy { it.fullName.lowercase() }
            .map { UserBriefResponse(id = it.id, fullName = it.fullName) }
            .toList()
    }

    fun getPendingUsers(actorId: Long): List<UserResponse> {
        accessControl.requireActiveAdmin(actorId)
        return userRepository.listPending().map { it.toResponse() }
    }

    fun createAdminUser(actorId: Long, body: CreateAdminUserRequest): UserResponse {
        accessControl.requireActiveAdmin(actorId)
        authValidator.validateRegister(
            RegisterRequest(
                fullName = body.fullName,
                email = body.email,
                password = body.password,
                requestedRole = UserRole.STOREKEEPER,
            ),
        )
        val email = body.email.trim().lowercase()
        if (userRepository.findByEmail(email) != null) {
            throw ConflictException("Пользователь с таким email уже существует")
        }
        val created =
            userRepository.create(
                email = email,
                passwordHash = passwordHasher.hash(body.password),
                fullName = body.fullName.trim(),
                role = UserRole.ADMIN,
                status = UserStatus.ACTIVE,
                now = dateTime.now(),
            )
        return created.toResponse()
    }

    fun approveUser(actorId: Long, targetId: Long): UserResponse {
        accessControl.requireActiveAdmin(actorId)
        val target = findTargetOrThrow(targetId)
        return when (target.status) {
            UserStatus.ACTIVE -> target.toResponse()
            UserStatus.BLOCKED ->
                throw ValidationException(
                    "Заблокированного пользователя сначала нужно разблокировать",
                )
            UserStatus.PENDING -> {
                userRepository.updateStatus(targetId, UserStatus.ACTIVE, dateTime.now())
                findTargetOrThrow(targetId).toResponse()
            }
        }
    }

    fun blockUser(actorId: Long, targetId: Long): UserResponse {
        accessControl.requireActiveAdmin(actorId)
        if (actorId == targetId) {
            throw ForbiddenException("Нельзя заблокировать собственную учетную запись")
        }
        val target = findTargetOrThrow(targetId)
        ensureNotLastActiveAdminBlock(target)
        if (target.status == UserStatus.BLOCKED) {
            return target.toResponse()
        }
        userRepository.updateStatus(targetId, UserStatus.BLOCKED, dateTime.now())
        return findTargetOrThrow(targetId).toResponse()
    }

    fun unblockUser(actorId: Long, targetId: Long): UserResponse {
        accessControl.requireActiveAdmin(actorId)
        val target = findTargetOrThrow(targetId)
        return when (target.status) {
            UserStatus.PENDING ->
                throw ValidationException(
                    "Пользователь ожидает подтверждения. Сначала подтвердите регистрацию.",
                )
            UserStatus.ACTIVE -> target.toResponse()
            UserStatus.BLOCKED -> {
                userRepository.updateStatus(targetId, UserStatus.ACTIVE, dateTime.now())
                findTargetOrThrow(targetId).toResponse()
            }
        }
    }

    fun changeUserRole(actorId: Long, targetId: Long, body: ChangeUserRoleRequest): UserResponse {
        accessControl.requireActiveAdmin(actorId)
        val newRole =
            runCatching { UserRole.valueOf(body.role.trim()) }.getOrElse {
                throw ValidationException("Некорректная роль")
            }
        val target = findTargetOrThrow(targetId)
        ensureNotLastActiveAdminDemote(target, newRole)
        userRepository.updateRole(targetId, newRole, dateTime.now())
        return findTargetOrThrow(targetId).toResponse()
    }
}
