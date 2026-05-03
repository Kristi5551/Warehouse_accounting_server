package com.example.warehouse_accounting_server

import com.example.warehouse_accounting_server.config.AppConfig
import com.example.warehouse_accounting_server.config.DatabaseConfig
import com.example.warehouse_accounting_server.config.HealthService
import com.example.warehouse_accounting_server.config.InitialDataSeed
import com.example.warehouse_accounting_server.config.ServerReadiness
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
import com.example.warehouse_accounting_server.domain.service.AccessControlService
import com.example.warehouse_accounting_server.domain.service.AuthService
import com.example.warehouse_accounting_server.domain.service.CategoryService
import com.example.warehouse_accounting_server.domain.service.ProductService
import com.example.warehouse_accounting_server.domain.service.ReportService
import com.example.warehouse_accounting_server.domain.service.StockService
import com.example.warehouse_accounting_server.domain.service.UserService
import com.example.warehouse_accounting_server.domain.validation.AuthValidator
import com.example.warehouse_accounting_server.domain.validation.ProductValidator
import com.example.warehouse_accounting_server.domain.validation.StockOperationValidator
import com.example.warehouse_accounting_server.dto.response.ErrorResponse
import com.example.warehouse_accounting_server.dto.response.health.HealthResponse
import com.example.warehouse_accounting_server.util.DateTimeProvider
import com.example.warehouse_accounting_server.util.JwtProvider
import com.example.warehouse_accounting_server.util.PasswordHasher
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun Application.module() {
    val appConfig = AppConfig.load(this)
    val jwtProvider = JwtProvider(appConfig.jwt)

    configureSerialization()
    configureStatusPages()
    configureCors()
    configureCallLogging()
    configureSecurity(appConfig, jwtProvider)

    /**
     * Порт открывается до завершения Flyway/БД. Пока API не готов — отвечаем 503, чтобы клиент
     * не ждал TCP connect timeout (как при полностью выключенном сервере).
     */
    intercept(ApplicationCallPipeline.Call) {
        val path = context.request.path()
        if (!ServerReadiness.isReady() && path.startsWith("/api") && path != "/api/health") {
            context.respond(
                HttpStatusCode.ServiceUnavailable,
                ErrorResponse(
                    message = "Сервер запускается, ожидайте подключения к базе данных.",
                    details = null,
                ),
            )
            finish()
        }
    }

    routing {
        get("/api/health") {
            try {
                val appReady = ServerReadiness.isReady()
                if (!appReady) {
                    try {
                        HealthService.pingDatabase()
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            HealthResponse(
                                status = "unavailable",
                                message = "Сервис временно недоступен",
                            ),
                        )
                    } catch (e: Exception) {
                        call.application.environment.log.warn("Health: база данных недоступна (сервер ещё не готов)", e)
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            HealthResponse(
                                status = "unavailable",
                                database = "unavailable",
                                message = "База данных недоступна",
                            ),
                        )
                    }
                } else {
                    try {
                        HealthService.pingDatabase()
                        call.respond(
                            HttpStatusCode.OK,
                            HealthResponse(status = "ok", database = "ok"),
                        )
                    } catch (e: Exception) {
                        call.application.environment.log.error("Health: проверка БД не удалась", e)
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            HealthResponse(
                                status = "unavailable",
                                database = "unavailable",
                                message = "База данных недоступна",
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                call.application.environment.log.error("Health: непредвиденная ошибка", e)
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    HealthResponse(
                        status = "unavailable",
                        message = "Сервис временно недоступен",
                    ),
                )
            }
        }
    }

    monitor.subscribe(ApplicationStarted) {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    DatabaseConfig.connect(appConfig.database)
                }
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
                InitialDataSeed.ensureMainWarehouse(warehouseRepository, dateTime)

                val authService = AuthService(userRepository, passwordHasher, jwtProvider, authValidator, dateTime)
                val userService = UserService(userRepository, dateTime, passwordHasher, authValidator)
                val accessControl = AccessControlService(userRepository)
                val categoryService = CategoryService(categoryRepository, dateTime, accessControl)
                val productService = ProductService(productRepository, productValidator, dateTime, accessControl, categoryRepository)
                val stockService = StockService(
                    stockRepository,
                    productRepository,
                    warehouseRepository,
                    userRepository,
                    stockOperationValidator,
                    dateTime,
                )
                val reportService = ReportService(reportRepository, stockRepository, userRepository)

                configureRouting(
                    authService = authService,
                    userService = userService,
                    categoryService = categoryService,
                    productService = productService,
                    stockService = stockService,
                    reportService = reportService,
                )
                ServerReadiness.setReady(true)
                environment.log.info("Warehouse API готов (БД и маршруты подключены).")
            } catch (e: Exception) {
                environment.log.error(
                    "Ошибка при старте БД/Flyway: порт слушается, /api вернёт 503 до устранения проблемы",
                    e,
                )
            }
        }
    }
}
