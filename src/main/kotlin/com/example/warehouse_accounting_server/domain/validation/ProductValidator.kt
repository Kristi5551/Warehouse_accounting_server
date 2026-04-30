package com.example.warehouse_accounting_server.domain.validation

import java.math.BigDecimal

class ProductValidator {
    fun parseMoney(raw: String, field: String): BigDecimal {
        val v = runCatching { BigDecimal(raw.trim()) }.getOrElse {
            throw IllegalArgumentException("Invalid $field")
        }
        require(v >= BigDecimal.ZERO) { "$field must be >= 0" }
        return v
    }

    fun parseQuantity(raw: String, field: String): BigDecimal {
        val v = runCatching { BigDecimal(raw.trim()) }.getOrElse {
            throw IllegalArgumentException("Invalid $field")
        }
        require(v > BigDecimal.ZERO) { "$field must be > 0" }
        return v
    }
}
