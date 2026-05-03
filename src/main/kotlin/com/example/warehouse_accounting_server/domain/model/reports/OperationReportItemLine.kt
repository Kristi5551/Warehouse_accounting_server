package com.example.warehouse_accounting_server.domain.model.reports

import java.math.BigDecimal

/** Одна позиция внутри складской операции в отчёте по движениям. */
data class OperationReportItemLine(
    val productArticle: String,
    val productName: String,
    val quantity: BigDecimal,
    val price: BigDecimal?,
)
