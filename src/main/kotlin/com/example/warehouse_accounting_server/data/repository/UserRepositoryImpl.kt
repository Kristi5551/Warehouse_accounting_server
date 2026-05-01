package com.example.warehouse_accounting_server.data.repository

import com.example.warehouse_accounting_server.data.mapper.UserMapper
import com.example.warehouse_accounting_server.data.table.UsersTable
import com.example.warehouse_accounting_server.domain.model.User
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class UserRepositoryImpl : UserRepository {
    override fun findById(id: Long): User? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.let(UserMapper::toDomain)
    }

    override fun findByEmail(email: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.email eq email }.singleOrNull()?.let(UserMapper::toDomain)
    }

    override fun listAll(): List<User> = transaction {
        UsersTable.selectAll().map(UserMapper::toDomain)
    }

    override fun listPending(): List<User> = transaction {
        UsersTable.selectAll().where { UsersTable.status eq UserStatus.PENDING.name }.map(UserMapper::toDomain)
    }

    override fun create(
        email: String,
        passwordHash: String,
        fullName: String,
        role: UserRole,
        status: UserStatus,
        now: LocalDateTime,
    ): User = transaction {
        val id = UsersTable.insertAndGetId {
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.fullName] = fullName
            it[UsersTable.role] = role.name
            it[UsersTable.status] = status.name
            it[createdAt] = now
            it[updatedAt] = now
        }
        UsersTable.selectAll().where { UsersTable.id eq id }.single().let(UserMapper::toDomain)
    }

    override fun updateStatus(id: Long, status: UserStatus, now: LocalDateTime): Boolean = transaction {
        UsersTable.update({ UsersTable.id eq id }) {
            it[UsersTable.status] = status.name
            it[updatedAt] = now
        } > 0
    }

    override fun updateRole(id: Long, role: UserRole, now: LocalDateTime): Boolean = transaction {
        UsersTable.update({ UsersTable.id eq id }) {
            it[UsersTable.role] = role.name
            it[updatedAt] = now
        } > 0
    }

    override fun countActiveAdmins(): Long = transaction {
        UsersTable
            .selectAll()
            .where {
                (UsersTable.role eq UserRole.ADMIN.name) and (UsersTable.status eq UserStatus.ACTIVE.name)
            }
            .count()
    }
}
