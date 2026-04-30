package com.example.warehouse_accounting_server.data.mapper

import com.example.warehouse_accounting_server.domain.model.Category
import com.example.warehouse_accounting_server.domain.model.Product
import com.example.warehouse_accounting_server.domain.model.StockBalance
import com.example.warehouse_accounting_server.domain.model.StockOperationItem
import com.example.warehouse_accounting_server.domain.model.StockStatus
import com.example.warehouse_accounting_server.domain.model.User
import com.example.warehouse_accounting_server.dto.response.category.CategoryResponse
import com.example.warehouse_accounting_server.dto.response.product.ProductResponse
import com.example.warehouse_accounting_server.dto.response.stock.StockBalanceResponse
import com.example.warehouse_accounting_server.dto.response.stock.StockOperationItemResponse
import com.example.warehouse_accounting_server.dto.response.user.UserResponse
import java.math.BigDecimal

fun User.toResponse(): UserResponse =
    UserResponse(
        id = id,
        email = email,
        fullName = fullName,
        role = role,
        status = status,
    )

fun Category.toResponse(): CategoryResponse =
    CategoryResponse(
        id = id,
        name = name,
        description = description,
        isActive = isActive,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

fun Product.toResponse(): ProductResponse =
    ProductResponse(
        id = id,
        article = article,
        name = name,
        categoryId = categoryId,
        categoryName = categoryName,
        unit = unit,
        purchasePrice = purchasePrice.stripTrailingZeros().toPlainString(),
        salePrice = salePrice.stripTrailingZeros().toPlainString(),
        minStock = minStock.stripTrailingZeros().toPlainString(),
        isActive = isActive,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

fun StockBalance.toBalanceResponse(
    productArticle: String,
    productName: String,
    categoryName: String?,
    warehouseName: String,
    minStock: BigDecimal,
    status: StockStatus,
): StockBalanceResponse =
    StockBalanceResponse(
        id = id,
        productId = productId,
        productArticle = productArticle,
        productName = productName,
        categoryName = categoryName,
        warehouseId = warehouseId,
        warehouseName = warehouseName,
        quantity = quantity.stripTrailingZeros().toPlainString(),
        minStock = minStock.stripTrailingZeros().toPlainString(),
        status = status.name,
        updatedAt = updatedAt.toString(),
    )

fun StockOperationItem.toItemResponse(): StockOperationItemResponse =
    StockOperationItemResponse(
        id = id,
        operationId = operationId,
        productId = productId,
        quantity = quantity.stripTrailingZeros().toPlainString(),
        price = price?.stripTrailingZeros()?.toPlainString(),
        reason = reason,
    )
