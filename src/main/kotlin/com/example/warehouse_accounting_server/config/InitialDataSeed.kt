package com.example.warehouse_accounting_server.config

import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.PasswordHasher

object InitialDataSeed {
    private const val ADMIN_EMAIL = "admin@warehouse.local"
    private const val ADMIN_PASSWORD = "admin123"

    fun ensureAdmin(
        userRepository: UserRepository,
        passwordHasher: PasswordHasher,
        dateTime: DateTimeProvider,
    ) {
        if (userRepository.findByEmail(ADMIN_EMAIL) != null) return
        userRepository.create(
            email = ADMIN_EMAIL,
            passwordHash = passwordHasher.hash(ADMIN_PASSWORD),
            fullName = "Системный администратор",
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE,
            now = dateTime.now(),
        )
    }
}
