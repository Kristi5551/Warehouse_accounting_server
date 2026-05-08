package com.example.warehouse_accounting_server.config

import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.domain.repository.WarehouseRepository
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.PasswordHasher

object InitialDataSeed {

    const val MAIN_WAREHOUSE_NAME = "Основной склад"

    private const val ADMIN_EMAIL = "admin@warehouse.local"

    private fun localDemoAdminPassword(): String =
        System.getenv("ADMIN_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() } ?: "admin123"

    fun ensureAdmin(
        userRepository: UserRepository,
        passwordHasher: PasswordHasher,
        dateTime: DateTimeProvider,
    ) {
        if (userRepository.findByEmail(ADMIN_EMAIL) != null) return
        userRepository.create(
            email = ADMIN_EMAIL,
            passwordHash = passwordHasher.hash(localDemoAdminPassword()),
            fullName = "Администратор системы",
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE,
            now = dateTime.now(),
        )
    }

    fun ensureMainWarehouse(
        warehouseRepository: WarehouseRepository,
        dateTime: DateTimeProvider,
    ) {
        if (warehouseRepository.findByName(MAIN_WAREHOUSE_NAME) != null) return
        warehouseRepository.create(MAIN_WAREHOUSE_NAME, address = null, now = dateTime.now())
    }
}
