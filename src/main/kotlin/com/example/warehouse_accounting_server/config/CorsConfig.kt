package com.example.warehouse_accounting_server.config

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

/**
 * CORS нужен для **браузерных** клиентов (SPA, devtools, Postman в браузере и т.п.).
 * Нативный **Android** обращается к API напрямую — **не зависит от CORS**.
 *
 * Список ниже — локальные origin для разработки. В **production** список нужно заменить
 * на реальные домены (например `allowHost("api.example.com", schemes = listOf("https"))`).
 */
fun Application.configureCors() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)

        val httpLocalPorts = listOf(3000, 5173, 8080)
        val localHosts = listOf("localhost", "127.0.0.1")
        for (host in localHosts) {
            for (port in httpLocalPorts) {
                allowHost("$host:$port", schemes = listOf("http"))
            }
        }
    }
}
