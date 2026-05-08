package com.example.warehouse_accounting_server.config

import java.util.concurrent.atomic.AtomicBoolean

object ServerReadiness {
    private val ready = AtomicBoolean(false)

    fun isReady(): Boolean = ready.get()

    internal fun setReady(value: Boolean) {
        ready.set(value)
    }
}
