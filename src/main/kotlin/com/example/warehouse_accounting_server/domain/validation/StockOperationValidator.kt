package com.example.warehouse_accounting_server.domain.validation

import java.math.BigDecimal

class StockOperationValidator(
    private val productValidator: ProductValidator,
) {
    fun parseQuantity(raw: String): BigDecimal = productValidator.parseQuantity(raw, "quantity")

    fun parseMoney(raw: String, field: String): BigDecimal = productValidator.parseMoney(raw, field)
}
