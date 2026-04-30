package com.example.warehouse_accounting_server.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    fun connect(settings: DatabaseSettings): HikariDataSource {
        FlywayConfig.migrate(settings.jdbcUrl, settings.user, settings.password)
        val hikari = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = settings.jdbcUrl
                username = settings.user
                password = settings.password
                maximumPoolSize = settings.maxPoolSize
            },
        )
        Database.connect(hikari)
        return hikari
    }
}
