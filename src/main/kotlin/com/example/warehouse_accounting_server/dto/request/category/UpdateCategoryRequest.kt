package com.example.warehouse_accounting_server.dto.request.category

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCategoryRequest(
    val name: String,
    val description: String? = null,
    val isActive: Boolean,
)
