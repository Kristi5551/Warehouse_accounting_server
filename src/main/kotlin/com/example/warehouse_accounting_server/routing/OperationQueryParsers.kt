package com.example.warehouse_accounting_server.routing

import com.example.warehouse_accounting_server.config.ValidationException
import com.example.warehouse_accounting_server.domain.model.StockOperationType
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun parseOperationTypeQuery(raw: String?): StockOperationType? {
    if (raw.isNullOrBlank()) return null
    return try {
        StockOperationType.valueOf(raw.trim())
    } catch (_: IllegalArgumentException) {
        throw ValidationException("Неизвестный тип операции: $raw")
    }
}

/**
 * Разбор query-параметров дат: **ISO-8601 календарная дата** `yyyy-MM-dd` (линейный формат, без локали).
 * Границы периода в SQL строятся на сервере через [com.example.warehouse_accounting_server.util.ReportDateBounds].
 */
fun parseDateQuery(raw: String?): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw.trim())
    } catch (_: DateTimeParseException) {
        throw ValidationException("Неверный формат даты, ожидается YYYY-MM-DD")
    }
}
