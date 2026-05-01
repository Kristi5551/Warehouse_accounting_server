package com.example.warehouse_accounting_server.domain.repository

import com.example.warehouse_accounting_server.domain.model.User
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus

interface UserRepository {
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun listAll(): List<User>
    fun listPending(): List<User>
    fun create(
        email: String,
        passwordHash: String,
        fullName: String,
        role: UserRole,
        status: UserStatus,
        now: java.time.LocalDateTime,
    ): User

    fun updateStatus(id: Long, status: UserStatus, now: java.time.LocalDateTime): Boolean
    fun updateRole(id: Long, role: UserRole, now: java.time.LocalDateTime): Boolean
    fun countActiveAdmins(): Long
}
