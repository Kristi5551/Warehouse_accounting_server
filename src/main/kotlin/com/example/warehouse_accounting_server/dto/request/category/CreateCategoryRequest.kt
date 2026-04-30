package com.example.warehouse_accounting_server.dto.request.category

import kotlinx.serialization.Serializable

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val description: String? = null,
)
