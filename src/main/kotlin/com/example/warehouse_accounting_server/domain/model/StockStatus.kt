package com.example.warehouse_accounting_server.domain.model

import java.math.BigDecimal

enum class StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
}

fun stockStatusFor(quantity: BigDecimal, minStock: BigDecimal): StockStatus = when {
    quantity.compareTo(BigDecimal.ZERO) == 0 -> StockStatus.OUT_OF_STOCK
    quantity <= minStock -> StockStatus.LOW_STOCK
    else -> StockStatus.IN_STOCK
}
