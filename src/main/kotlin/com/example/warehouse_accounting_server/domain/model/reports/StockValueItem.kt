package com.example.warehouse_accounting_server.domain.model.reports

import java.math.BigDecimal

data class StockValueItem(
    val productId: Long,
    val productArticle: String,
    val productName: String,
    val quantity: BigDecimal,
    val purchasePrice: BigDecimal,
    val value: BigDecimal,
)
