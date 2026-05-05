package com.example.warehouse_accounting_server.util

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Границы периода для отчётов и истории операций.
 *
 * Календарные query-параметры `dateFrom` / `dateTo` — ISO **yyyy-MM-dd**, без времени.
 * Интерпретация:
 * - **fromInclusive** — `dateFrom.atStartOfDay()` в локальной зоне **JVM сервера** (та же семантика, что и у `TIMESTAMP WITHOUT TIME ZONE` в PostgreSQL для `created_at` в этом проекте).
 * - **toExclusive** — `dateTo.plusDays(1).atStartOfDay()`: в выборку попадает весь последний день **включительно** (`createdAt < toExclusive`).
 *
 * Отдельной таймзоны приложения нет: дата — «настенный календарь сервера/БД». При необходимости учёта часовых поясов пользователя понадобится явная политика (например UTC + смещение в API).
 *
 * Подробный контракт для клиентов: **`API_DATE_RANGE.md`** в корне модуля сервера.
 */
data class ReportDateBounds(
    val fromInclusive: LocalDateTime?,
    val toExclusive: LocalDateTime?,
) {
    companion object {
        fun from(dateFrom: LocalDate?, dateTo: LocalDate?): ReportDateBounds =
            ReportDateBounds(
                fromInclusive = dateFrom?.atStartOfDay(),
                toExclusive = dateTo?.plusDays(1)?.atStartOfDay(),
            )
    }
}
