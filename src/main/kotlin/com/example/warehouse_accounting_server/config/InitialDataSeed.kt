package com.example.warehouse_accounting_server.config

import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.CategoryRepository
import com.example.warehouse_accounting_server.domain.repository.ProductRepository
import com.example.warehouse_accounting_server.domain.repository.StockRepository
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.domain.repository.WarehouseRepository
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.PasswordHasher
import java.math.BigDecimal

object InitialDataSeed {
    const val MAIN_WAREHOUSE_NAME = "Основной склад"

    private const val ADMIN_EMAIL = "admin@warehouse.local"
    private const val ADMIN_PASSWORD = "admin123"

    private val SEED_CATEGORIES = listOf(
        Pair("Компьютерная периферия", "Клавиатуры, мыши, мониторы и аксессуары"),
        Pair("Канцелярия", "Ручки, тетради, бумага и офисные принадлежности"),
        Pair("Бытовая техника", "Электроприборы для дома и офиса"),
        Pair("Аксессуары", "Разные вспомогательные аксессуары"),
    )

    private data class ProductSeed(
        val article: String,
        val name: String,
        val categoryName: String,
        val unit: String,
        val purchasePrice: BigDecimal,
        val salePrice: BigDecimal,
        val minStock: BigDecimal,
    )

    private val SEED_PRODUCTS = listOf(
        ProductSeed("KB-001", "Клавиатура Logitech K120", "Компьютерная периферия", "шт.",
            BigDecimal("1200.00"), BigDecimal("1800.00"), BigDecimal("5.000")),
        ProductSeed("MS-001", "Мышь беспроводная", "Компьютерная периферия", "шт.",
            BigDecimal("800.00"), BigDecimal("1200.00"), BigDecimal("5.000")),
        ProductSeed("PA-001", "Бумага A4 (500 листов)", "Канцелярия", "уп.",
            BigDecimal("350.00"), BigDecimal("500.00"), BigDecimal("20.000")),
        ProductSeed("USB-001", "USB-накопитель 64 ГБ", "Аксессуары", "шт.",
            BigDecimal("600.00"), BigDecimal("900.00"), BigDecimal("10.000")),
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

    fun ensureProducts(
        productRepository: ProductRepository,
        categoryRepository: CategoryRepository,
        dateTime: DateTimeProvider,
    ) {
        for (seed in SEED_PRODUCTS) {
            if (productRepository.existsByArticle(seed.article)) continue
            val category = categoryRepository.findByName(seed.categoryName) ?: continue
            productRepository.create(
                article = seed.article,
                name = seed.name,
                categoryId = category.id,
                unit = seed.unit,
                purchasePrice = seed.purchasePrice,
                salePrice = seed.salePrice,
                minStock = seed.minStock,
                now = dateTime.now(),
            )
        }
    }

    fun ensureMainWarehouse(warehouseRepository: WarehouseRepository, dateTime: DateTimeProvider) {
        if (warehouseRepository.findByName(MAIN_WAREHOUSE_NAME) != null) return
        warehouseRepository.create(MAIN_WAREHOUSE_NAME, address = null, now = dateTime.now())
    }

    fun ensureStockBalances(
        productRepository: ProductRepository,
        stockRepository: StockRepository,
        warehouseRepository: WarehouseRepository,
        dateTime: DateTimeProvider,
    ) {
        val warehouse = warehouseRepository.findByName(MAIN_WAREHOUSE_NAME) ?: return
        for (product in productRepository.findAll(activeOnly = true)) {
            stockRepository.createBalanceIfMissing(product.id, warehouse.id, dateTime.now())
        }
    }
}
