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
    fun list(activeOnly: Boolean = true): List<CategoryResponse> =
        categoryRepository.findAll(includeInactive = !activeOnly).map { it.toResponse() }

    fun getById(id: Long): CategoryResponse =
        categoryRepository.findById(id)?.toResponse()
            ?: throw ApiException(HttpStatusCode.NotFound, "Категория не найдена")

    fun create(request: CreateCategoryRequest): CategoryResponse {
        val name = request.name.trim()
        if (name.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Название категории не может быть пустым")
        if (categoryRepository.existsByName(name)) {
            throw ApiException(HttpStatusCode.Conflict, "Категория с таким названием уже существует")
        }
        return categoryRepository.create(
            name = name,
            description = request.description?.trim()?.takeIf { it.isNotBlank() },
            now = dateTime.now(),
        ).toResponse()
    }

    fun update(id: Long, request: UpdateCategoryRequest): CategoryResponse {
        val name = request.name.trim()
        if (name.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Название категории не может быть пустым")
        categoryRepository.findById(id)
            ?: throw ApiException(HttpStatusCode.NotFound, "Категория не найдена")
        if (categoryRepository.existsByName(name, excludeId = id)) {
            throw ApiException(HttpStatusCode.Conflict, "Категория с таким названием уже существует")
        }
        return (
            categoryRepository.update(
                id = id,
                name = name,
                description = request.description?.trim()?.takeIf { it.isNotBlank() },
                isActive = request.isActive,
                now = dateTime.now(),
            ) ?: throw ApiException(HttpStatusCode.NotFound, "Категория не найдена")
            ).toResponse()
    }

    fun deactivate(id: Long): CategoryResponse {
        categoryRepository.findById(id)
            ?: throw ApiException(HttpStatusCode.NotFound, "Категория не найдена")
        return categoryRepository.deactivate(id, dateTime.now())?.toResponse()
            ?: throw ApiException(HttpStatusCode.NotFound, "Категория не найдена")
    }
}
