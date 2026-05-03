package com.example.warehouse_accounting_server.config

import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.domain.repository.WarehouseRepository
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.PasswordHasher

/**
 * Идемпотентный начальный сид критических данных, которые не поставляются Flyway-миграциями:
 * - учётная запись администратора (пароль хранится только BCrypt-хешем);
 * - основной склад (на случай, если миграции ещё не создали его).
 *
 * Демо-данные (категории, товары, остатки) поставляются миграцией Flyway V8.
 * Каждый вызов безопасен для повторного запуска — дубликаты не создаются.
 */
object InitialDataSeed {

    const val MAIN_WAREHOUSE_NAME = "Основной склад"

    private const val ADMIN_EMAIL = "admin@warehouse.local"
    private const val ADMIN_PASSWORD = "admin123"

    /**
     * Создаёт начального администратора, если пользователь с таким email ещё не существует.
     *
     * Учётные данные по умолчанию:
     *   email:    admin@warehouse.local
     *   password: admin123
     *   role:     ADMIN
     *   status:   ACTIVE
     */
    fun ensureAdmin(
        userRepository: UserRepository,
        passwordHasher: PasswordHasher,
        dateTime: DateTimeProvider,
    ) {
        if (userRepository.findByEmail(ADMIN_EMAIL) != null) return
        userRepository.create(
            email = ADMIN_EMAIL,
            passwordHash = passwordHasher.hash(ADMIN_PASSWORD),
            fullName = "Администратор системы",
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE,
            now = dateTime.now(),
        )
    }

    /**
     * Создаёт основной склад, если он ещё не существует.
     * Flyway V8 создаёт «Главный склад», V9 переименовывает его в «Основной склад».
     * Этот метод — страховка на случай, если миграции не были применены.
     */
    fun ensureMainWarehouse(
        warehouseRepository: WarehouseRepository,
        dateTime: DateTimeProvider,
    ) {
        if (warehouseRepository.findByName(MAIN_WAREHOUSE_NAME) != null) return
        warehouseRepository.create(MAIN_WAREHOUSE_NAME, address = null, now = dateTime.now())
    }
}
