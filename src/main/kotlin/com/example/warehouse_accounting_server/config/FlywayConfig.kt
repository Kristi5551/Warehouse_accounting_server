package com.example.warehouse_accounting_server.config

import org.flywaydb.core.Flyway
import java.util.Locale

object FlywayConfig {
    fun migrate(jdbcUrl: String, user: String, password: String) {
        val config =
            Flyway.configure()
                .dataSource(jdbcUrl, user, password)
                .locations("classpath:db/migration")
        val flyway = config.load()
        if (flywayRepairOnMigrate()) {
            flyway.repair()
        }
        flyway.migrate()
    }

    private fun flywayRepairOnMigrate(): Boolean {
        val env = System.getenv("APP_ENV")?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return env !in setOf("production", "prod", "staging")
    }
}
