package com.example.warehouse_accounting_server.util

import java.time.LocalDateTime
import java.time.ZoneOffset

class DateTimeProvider(
    private val zone: ZoneOffset = ZoneOffset.UTC,
) {
    fun now(): LocalDateTime = LocalDateTime.now(zone)
}
