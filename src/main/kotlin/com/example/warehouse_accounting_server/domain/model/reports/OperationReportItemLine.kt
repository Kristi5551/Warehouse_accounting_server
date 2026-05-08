package com.example.warehouse_accounting_server.domain.model.reports

import java.math.BigDecimal

data class OperationReportItemLine(
    val productArticle: String,
    val productName: String,
    val quantity: BigDecimal,
    val price: BigDecimal?,
)
