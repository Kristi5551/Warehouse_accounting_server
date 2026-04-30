package com.example.warehouse_accounting_server.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging

fun Application.configureCallLogging() {
    install(CallLogging)
}
