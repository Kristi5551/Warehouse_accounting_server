package com.example.warehouse_accounting_server.util

import java.time.LocalDate
import java.time.LocalDateTime

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
