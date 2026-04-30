package com.example.warehouse_accounting_server.domain.model

data class StockOperationWithItems(
    val operation: StockOperation,
    val items: List<StockOperationItem>,
)
