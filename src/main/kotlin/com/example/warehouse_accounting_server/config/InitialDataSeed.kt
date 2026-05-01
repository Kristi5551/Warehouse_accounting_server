package com.example.warehouse_accounting_server.config

import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.CategoryRepository
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.PasswordHasher

object InitialDataSeed {
    private const val ADMIN_EMAIL = "admin@warehouse.local"
    private const val ADMIN_PASSWORD = "admin123"

    private val SEED_CATEGORIES = listOf(
        Pair("Компьютерная периферия", "Клавиатуры, мыши, мониторы и аксессуары"),
        Pair("Канцелярия", "Ручки, тетради, бумага и офисные принадлежности"),
        Pair("Бытовая техника", "Электроприборы для дома и офиса"),
        Pair("Аксессуары", "Разные вспомогательные аксессуары"),
    )

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

    fun ensureCategories(
        categoryRepository: CategoryRepository,
        dateTime: DateTimeProvider,
    ) {
        for ((name, description) in SEED_CATEGORIES) {
            if (!categoryRepository.existsByName(name)) {
                categoryRepository.create(name = name, description = description, now = dateTime.now())
            }
        }
    }
}
