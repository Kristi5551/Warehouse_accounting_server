package com.example.warehouse_accounting_server.config

import java.util.concurrent.atomic.AtomicBoolean

/** Пока false — API ещё не готов (БД/Flyway), порт уже слушается. */
object ServerReadiness {
    private val ready = AtomicBoolean(false)

    fun isReady(): Boolean = ready.get()

    internal fun setReady(value: Boolean) {
        ready.set(value)
    }
}
