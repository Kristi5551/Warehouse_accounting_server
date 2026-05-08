package com.example.warehouse_accounting_server.domain.model

import java.math.BigDecimal

data class StockOperationItem(
    val id: Long,
    val operationId: Long,
    val productId: Long,
    val quantity: BigDecimal,
    val price: BigDecimal?,
    val reason: String?,
)
