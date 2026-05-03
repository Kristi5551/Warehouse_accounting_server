package com.example.warehouse_accounting_server.dto.response.report

import kotlinx.serialization.Serializable

@Serializable
data class OperationReportItemResponse(
    val productArticle: String,
    val productName: String,
    val quantity: String,
    val price: String? = null,
)
