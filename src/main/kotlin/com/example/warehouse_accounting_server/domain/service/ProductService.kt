package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ApiException
import com.example.warehouse_accounting_server.data.mapper.toResponse
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
) {
    fun list(categoryId: Long?, search: String?): List<ProductResponse> =
        productRepository.findAll(
            categoryId = categoryId,
            search = search,
            includeInactive = false,
        ).map { it.toResponse() }

    fun adminList(categoryId: Long?, search: String?): List<ProductResponse> =
        productRepository.findAll(
            categoryId = categoryId,
            search = search,
            includeInactive = true,
        ).map { it.toResponse() }

    fun getById(id: Long): ProductResponse {
        val p = productRepository.findById(id)
            ?: throw ApiException(HttpStatusCode.NotFound, "Product not found")
        return p.toResponse()
    }

    fun create(request: CreateProductRequest): ProductResponse {
        val p = productRepository.create(
            article = request.article.trim(),
            name = request.name.trim(),
            categoryId = request.categoryId,
            unit = request.unit.trim(),
            purchasePrice = productValidator.parseMoney(request.purchasePrice, "purchasePrice"),
            salePrice = productValidator.parseMoney(request.salePrice, "salePrice"),
            minStock = productValidator.parseMoney(request.minStock, "minStock"),
            now = dateTime.now(),
        )
        return p.toResponse()
    }

    fun update(id: Long, request: UpdateProductRequest): ProductResponse {
        val p = productRepository.update(
            id = id,
            article = request.article.trim(),
            name = request.name.trim(),
            categoryId = request.categoryId,
            unit = request.unit.trim(),
            purchasePrice = productValidator.parseMoney(request.purchasePrice, "purchasePrice"),
            salePrice = productValidator.parseMoney(request.salePrice, "salePrice"),
            minStock = productValidator.parseMoney(request.minStock, "minStock"),
            isActive = request.isActive,
            now = dateTime.now(),
        ) ?: throw ApiException(HttpStatusCode.NotFound, "Product not found")
        return p.toResponse()
    }

    fun deactivate(id: Long) {
        val ok = productRepository.deactivate(id, dateTime.now())
        if (!ok) throw ApiException(HttpStatusCode.NotFound, "Product not found")
    }
}
