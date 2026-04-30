package com.example.warehouse_accounting_server

import com.example.warehouse_accounting_server.config.AppConfig
import com.example.warehouse_accounting_server.config.DatabaseConfig
import com.example.warehouse_accounting_server.config.InitialDataSeed
import com.example.warehouse_accounting_server.config.configureCallLogging
import com.example.warehouse_accounting_server.config.configureCors
import com.example.warehouse_accounting_server.config.configureRouting
import com.example.warehouse_accounting_server.config.configureSecurity
import com.example.warehouse_accounting_server.config.configureSerialization
import com.example.warehouse_accounting_server.config.configureStatusPages
import com.example.warehouse_accounting_server.data.repository.CategoryRepositoryImpl
import com.example.warehouse_accounting_server.data.repository.ProductRepositoryImpl
import com.example.warehouse_accounting_server.data.repository.ReportRepositoryImpl
import com.example.warehouse_accounting_server.data.repository.StockRepositoryImpl
import com.example.warehouse_accounting_server.data.repository.UserRepositoryImpl
import com.example.warehouse_accounting_server.data.repository.WarehouseRepositoryImpl
import com.example.warehouse_accounting_server.domain.service.AuthService
import com.example.warehouse_accounting_server.domain.service.CategoryService
import com.example.warehouse_accounting_server.domain.service.ProductService
import com.example.warehouse_accounting_server.domain.service.ReportService
import com.example.warehouse_accounting_server.domain.service.StockService
import com.example.warehouse_accounting_server.domain.service.UserService
import com.example.warehouse_accounting_server.domain.validation.AuthValidator
import com.example.warehouse_accounting_server.domain.validation.ProductValidator
import com.example.warehouse_accounting_server.domain.validation.StockOperationValidator
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.JwtProvider
import com.example.warehouse_accounting_server.util.PasswordHasher
import io.ktor.server.application.Application

fun Application.module() {
    val appConfig = AppConfig.load(this)

    DatabaseConfig.connect(appConfig.database)

    val jwtProvider = JwtProvider(appConfig.jwt)
    val passwordHasher = PasswordHasher()
    val dateTime = DateTimeProvider()
    val authValidator = AuthValidator()
    val productValidator = ProductValidator()
    val stockOperationValidator = StockOperationValidator(productValidator)

    val userRepository = UserRepositoryImpl()
    val categoryRepository = CategoryRepositoryImpl()
    val productRepository = ProductRepositoryImpl()
    val warehouseRepository = WarehouseRepositoryImpl()
    val stockRepository = StockRepositoryImpl()
    val reportRepository = ReportRepositoryImpl()

    InitialDataSeed.ensureAdmin(userRepository, passwordHasher, dateTime)

    val authService = AuthService(userRepository, passwordHasher, jwtProvider, authValidator, dateTime)
    val userService = UserService(userRepository, dateTime)
    val categoryService = CategoryService(categoryRepository, dateTime)
    val productService = ProductService(productRepository, productValidator, dateTime)
    val stockService = StockService(
        stockRepository,
        productRepository,
        warehouseRepository,
        userRepository,
        stockOperationValidator,
        dateTime,
    )
    val reportService = ReportService(reportRepository)

    configureSerialization()
    configureStatusPages()
    configureCors()
    configureCallLogging()
    configureSecurity(appConfig, jwtProvider)
    configureRouting(
        authService = authService,
        userService = userService,
        categoryService = categoryService,
        productService = productService,
        stockService = stockService,
        reportService = reportService,
    )
}
