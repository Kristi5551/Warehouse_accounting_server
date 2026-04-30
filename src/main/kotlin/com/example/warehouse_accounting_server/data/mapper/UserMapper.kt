package com.example.warehouse_accounting_server.data.mapper

import com.example.warehouse_accounting_server.data.table.UsersTable
import com.example.warehouse_accounting_server.domain.model.User
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import org.jetbrains.exposed.sql.ResultRow

object UserMapper {
    fun toDomain(row: ResultRow): User =
        User(
            id = row[UsersTable.id].value,
            email = row[UsersTable.email],
            passwordHash = row[UsersTable.passwordHash],
            fullName = row[UsersTable.fullName],
            role = UserRole.valueOf(row[UsersTable.role]),
            status = UserStatus.valueOf(row[UsersTable.status]),
            createdAt = row[UsersTable.createdAt],
            updatedAt = row[UsersTable.updatedAt],
        )
}
