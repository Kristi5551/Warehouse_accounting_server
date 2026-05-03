package com.example.warehouse_accounting_server.domain.validation

import java.math.BigDecimal

class ProductValidator {

    private fun invalidFormatMessage(field: String): String =
        when (field) {
            "quantity" -> "Количество указано некорректно"
            "price" -> "Цена указана некорректно"
            "purchasePrice" -> "Закупочная цена указана некорректно"
            "salePrice" -> "Цена продажи указана некорректно"
            "minStock" -> "Минимальный остаток указан некорректно"
            "actualQuantity" -> "Фактическое количество указано некорректно"
            else -> "Значение указано некорректно"
        }

    private fun mustBeNonNegativeMessage(field: String): String =
        when (field) {
            "quantity" -> "Количество не может быть отрицательным"
            "price" -> "Цена не может быть отрицательной"
            "purchasePrice" -> "Закупочная цена не может быть отрицательной"
            "salePrice" -> "Цена продажи не может быть отрицательной"
            "minStock" -> "Минимальный остаток не может быть отрицательным"
            "actualQuantity" -> "Фактическое количество не может быть отрицательным"
            else -> "Значение не может быть отрицательным"
        }

    private fun mustBePositiveMessage(field: String): String =
        when (field) {
            "quantity" -> "Количество должно быть больше нуля"
            "price" -> "Цена должна быть больше нуля"
            "purchasePrice" -> "Закупочная цена должна быть больше нуля"
            "salePrice" -> "Цена продажи должна быть больше нуля"
            "minStock" -> "Минимальный остаток должен быть больше нуля"
            "actualQuantity" -> "Фактическое количество должно быть больше нуля"
            else -> "Значение должно быть больше нуля"
        }

    fun parseMoney(raw: String, field: String): BigDecimal {
        val v = runCatching { BigDecimal(raw.trim()) }.getOrElse {
            throw IllegalArgumentException(invalidFormatMessage(field))
        }
        require(v >= BigDecimal.ZERO) { mustBeNonNegativeMessage(field) }
        return v
    }

    fun parseQuantity(raw: String, field: String): BigDecimal {
        val v = runCatching { BigDecimal(raw.trim()) }.getOrElse {
            throw IllegalArgumentException(invalidFormatMessage(field))
        }
        require(v > BigDecimal.ZERO) { mustBePositiveMessage(field) }
        return v
    }

    /** Для инвентаризации и полей, где допустим ноль. */
    fun parseNonNegativeQuantity(raw: String, field: String): BigDecimal {
        val v = runCatching { BigDecimal(raw.trim()) }.getOrElse {
            throw IllegalArgumentException(invalidFormatMessage(field))
        }
        require(v >= BigDecimal.ZERO) { mustBeNonNegativeMessage(field) }
        return v
    }
}
