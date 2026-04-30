package com.example.warehouse_accounting_server

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val env = applicationEnvironment {
        config = ApplicationConfig("application.conf")
    }
    embeddedServer(Netty, env, module = Application::module).start(wait = true)
}
