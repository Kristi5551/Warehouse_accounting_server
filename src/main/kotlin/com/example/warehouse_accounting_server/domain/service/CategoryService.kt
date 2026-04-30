package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.data.mapper.toResponse
import com.example.warehouse_accounting_server.domain.repository.CategoryRepository
import com.example.warehouse_accounting_server.dto.request.category.CreateCategoryRequest
import com.example.warehouse_accounting_server.dto.request.category.UpdateCategoryRequest
import com.example.warehouse_accounting_server.dto.response.category.CategoryResponse
import com.example.warehouse_accounting_server.util.DateTimeProvider
import io.ktor.http.HttpStatusCode

class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val dateTime: DateTimeProvider,
) {
    fun list(): List<CategoryResponse> =
        categoryRepository.findAll(includeInactive = true).map { it.toResponse() }

    fun create(request: CreateCategoryRequest): CategoryResponse {
        val c = categoryRepository.create(
            name = request.name.trim(),
            description = request.description?.trim(),
            now = dateTime.now(),
        )
        return c.toResponse()
    }

    fun update(id: Long, request: UpdateCategoryRequest): CategoryResponse {
        val updated = categoryRepository.update(
            id = id,
            name = request.name.trim(),
            description = request.description?.trim(),
            isActive = request.isActive,
            now = dateTime.now(),
        ) ?: throw ApiException(HttpStatusCode.NotFound, "Category not found")
        return updated.toResponse()
    }

    fun deactivate(id: Long) {
        val ok = categoryRepository.deactivate(id, dateTime.now())
        if (!ok) throw ApiException(HttpStatusCode.NotFound, "Category not found")
    }
}
