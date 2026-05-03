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

fun parseDateQuery(raw: String?): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw.trim())
    } catch (_: DateTimeParseException) {
        throw ValidationException("Неверный формат даты, ожидается YYYY-MM-DD")
    }
}
