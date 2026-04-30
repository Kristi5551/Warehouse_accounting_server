package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.data.mapper.toResponse
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.dto.request.user.ApproveUserRequest
import com.example.warehouse_accounting_server.dto.request.user.ChangeUserRoleRequest
import com.example.warehouse_accounting_server.dto.response.user.UserResponse
import com.example.warehouse_accounting_server.util.DateTimeProvider
import io.ktor.http.HttpStatusCode

class UserService(
    private val userRepository: UserRepository,
    private val dateTime: DateTimeProvider,
) {
    fun list(): List<UserResponse> = userRepository.listAll().map { it.toResponse() }

    fun listPending(): List<UserResponse> = userRepository.listPending().map { it.toResponse() }

    fun approve(id: Long, @Suppress("UNUSED_PARAMETER") request: ApproveUserRequest) {
        val ok = userRepository.updateStatus(id, UserStatus.ACTIVE, dateTime.now())
        if (!ok) throw ApiException(HttpStatusCode.NotFound, "User not found")
    }

    fun block(id: Long) {
        val ok = userRepository.updateStatus(id, UserStatus.BLOCKED, dateTime.now())
        if (!ok) throw ApiException(HttpStatusCode.NotFound, "User not found")
    }

    fun unblock(id: Long) {
        val ok = userRepository.updateStatus(id, UserStatus.ACTIVE, dateTime.now())
        if (!ok) throw ApiException(HttpStatusCode.NotFound, "User not found")
    }

    fun changeRole(id: Long, body: ChangeUserRoleRequest) {
        val role = runCatching { UserRole.valueOf(body.role) }.getOrElse {
            throw ApiException(HttpStatusCode.BadRequest, "Invalid role")
        }
        val ok = userRepository.updateRole(id, role, dateTime.now())
        if (!ok) throw ApiException(HttpStatusCode.NotFound, "User not found")
    }
}
