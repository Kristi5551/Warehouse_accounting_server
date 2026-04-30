package com.example.warehouse_accounting_server.config

import org.flywaydb.core.Flyway

object FlywayConfig {
    fun migrate(jdbcUrl: String, user: String, password: String) {
        Flyway.configure()
            .dataSource(jdbcUrl, user, password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}
