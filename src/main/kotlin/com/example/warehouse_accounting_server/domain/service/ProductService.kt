package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.data.mapper.toResponse
import com.example.warehouse_accounting_server.domain.repository.CategoryRepository
import com.example.warehouse_accounting_server.domain.repository.ProductRepository
import com.example.warehouse_accounting_server.domain.validation.ProductValidator
import com.example.warehouse_accounting_server.dto.request.product.CreateProductRequest
import com.example.warehouse_accounting_server.dto.request.product.UpdateProductRequest
import com.example.warehouse_accounting_server.dto.response.product.ProductResponse
import com.example.warehouse_accounting_server.util.DateTimeProvider
import io.ktor.http.HttpStatusCode

class ProductService(
    private val productRepository: ProductRepository,
    private val productValidator: ProductValidator,
    private val dateTime: DateTimeProvider,
    private val accessControl: AccessControlService,
    private val categoryRepository: CategoryRepository? = null,
) {
    fun list(currentUserId: Long, search: String?, categoryId: Long?, activeOnly: Boolean = true): List<ProductResponse> {
        accessControl.requireActiveUser(currentUserId)
        return productRepository.findAll(search = search, categoryId = categoryId, activeOnly = activeOnly)
            .map { it.toResponse() }
    }

    fun getById(currentUserId: Long, id: Long): ProductResponse {
        accessControl.requireActiveUser(currentUserId)
        return productRepository.findById(id)?.toResponse()
            ?: throw ApiException(HttpStatusCode.NotFound, "Товар не найден")
    }

    fun create(currentUserId: Long, request: CreateProductRequest): ProductResponse {
        accessControl.requireActiveAdmin(currentUserId)
        val article = request.article.trim()
        val name = request.name.trim()
        val unit = request.unit.trim()

        if (article.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Артикул обязателен")
        if (name.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Название товара обязательно")
        if (unit.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Единица измерения обязательна")

        if (productRepository.existsByArticle(article)) {
            throw ApiException(HttpStatusCode.Conflict, "Товар с таким артикулом уже существует")
        }

        categoryRepository?.let { repo ->
            val cat = repo.findById(request.categoryId)
                ?: throw ApiException(HttpStatusCode.BadRequest, "Категория не найдена")
            if (!cat.isActive) throw ApiException(HttpStatusCode.BadRequest, "Категория неактивна")
        }

        val purchasePrice = productValidator.parseMoney(request.purchasePrice, "purchasePrice")
        val salePrice = productValidator.parseMoney(request.salePrice, "salePrice")
        val minStock = productValidator.parseMoney(request.minStock, "minStock")

        return productRepository.create(
            article = article,
            name = name,
            categoryId = request.categoryId,
            unit = unit,
            purchasePrice = purchasePrice,
            salePrice = salePrice,
            minStock = minStock,
            now = dateTime.now(),
        ).toResponse()
    }

    fun update(currentUserId: Long, id: Long, request: UpdateProductRequest): ProductResponse {
        accessControl.requireActiveAdmin(currentUserId)
        val article = request.article.trim()
        val name = request.name.trim()
        val unit = request.unit.trim()

        if (article.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Артикул обязателен")
        if (name.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Название товара обязательно")
        if (unit.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Единица измерения обязательна")

        productRepository.findById(id)
            ?: throw ApiException(HttpStatusCode.NotFound, "Товар не найден")

        if (productRepository.existsByArticle(article, excludeId = id)) {
            throw ApiException(HttpStatusCode.Conflict, "Товар с таким артикулом уже существует")
        }

        categoryRepository?.let { repo ->
            val cat = repo.findById(request.categoryId)
                ?: throw ApiException(HttpStatusCode.BadRequest, "Категория не найдена")
            if (!cat.isActive) throw ApiException(HttpStatusCode.BadRequest, "Категория неактивна")
        }

        val purchasePrice = productValidator.parseMoney(request.purchasePrice, "purchasePrice")
        val salePrice = productValidator.parseMoney(request.salePrice, "salePrice")
        val minStock = productValidator.parseMoney(request.minStock, "minStock")

        return (
            productRepository.update(
                id = id,
                article = article,
                name = name,
                categoryId = request.categoryId,
                unit = unit,
                purchasePrice = purchasePrice,
                salePrice = salePrice,
                minStock = minStock,
                isActive = request.isActive,
                now = dateTime.now(),
            ) ?: throw ApiException(HttpStatusCode.NotFound, "Товар не найден")
            ).toResponse()
    }

    fun deactivate(currentUserId: Long, id: Long): ProductResponse {
        accessControl.requireActiveAdmin(currentUserId)
        productRepository.findById(id)
            ?: throw ApiException(HttpStatusCode.NotFound, "Товар не найден")
        return productRepository.deactivate(id, dateTime.now())?.toResponse()
            ?: throw ApiException(HttpStatusCode.NotFound, "Товар не найден")
    }
}
