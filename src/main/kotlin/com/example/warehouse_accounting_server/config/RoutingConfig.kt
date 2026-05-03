package com.example.warehouse_accounting_server.config

import com.example.warehouse_accounting_server.domain.service.AuthService
import com.example.warehouse_accounting_server.domain.service.CategoryService
import com.example.warehouse_accounting_server.domain.service.ProductService
import com.example.warehouse_accounting_server.domain.service.ReportService
import com.example.warehouse_accounting_server.domain.service.StockService
import com.example.warehouse_accounting_server.domain.service.UserService
import com.example.warehouse_accounting_server.routing.authRoutes
import com.example.warehouse_accounting_server.routing.categoryRoutes
import com.example.warehouse_accounting_server.routing.operationRoutes
import com.example.warehouse_accounting_server.routing.productRoutes
import com.example.warehouse_accounting_server.routing.reportRoutes
import com.example.warehouse_accounting_server.routing.stockRoutes
import com.example.warehouse_accounting_server.routing.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(
    authService: AuthService,
    userService: UserService,
    categoryService: CategoryService,
    productService: ProductService,
    stockService: StockService,
    reportService: ReportService,
) {
    routing {
        authRoutes(authService)
        userRoutes(userService)
        categoryRoutes(categoryService)
        productRoutes(productService)
        stockRoutes(stockService)
        operationRoutes(stockService)
        reportRoutes(reportService)
    }
}
