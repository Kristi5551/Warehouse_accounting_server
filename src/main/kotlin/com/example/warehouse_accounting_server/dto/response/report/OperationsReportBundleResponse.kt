package com.example.warehouse_accounting_server.dto.response.report

import kotlinx.serialization.Serializable

@Serializable
data class OperationsReportBundleResponse(
    val operations: List<OperationReportResponse>,
    val receiptCount: Int,
    val issueCount: Int,
    val writeOffCount: Int,
    val inventoryCount: Int,
)
