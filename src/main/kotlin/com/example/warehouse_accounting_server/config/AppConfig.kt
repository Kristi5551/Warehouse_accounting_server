package com.example.warehouse_accounting_server.config

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig

data class DatabaseSettings(
    val jdbcUrl: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
)

data class JwtSettings(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expirationMillis: Long,
)

data class AppConfig(
    val database: DatabaseSettings,
    val jwt: JwtSettings,
) {
    companion object {
        fun load(app: Application): AppConfig {
            return from(app.environment.config)
        }

        fun from(config: ApplicationConfig): AppConfig {
            val root = config.config("warehouse")
            val db = root.config("database")
            val jwt = root.config("jwt")
            return AppConfig(
                database = DatabaseSettings(
                    jdbcUrl = db.property("jdbcUrl").getString(),
                    user = db.property("user").getString(),
                    password = db.property("password").getString(),
                    maxPoolSize = db.property("maxPoolSize").getString().toInt(),
                ),
                jwt = JwtSettings(
                    secret = jwt.property("secret").getString(),
                    issuer = jwt.property("issuer").getString(),
                    audience = jwt.property("audience").getString(),
                    realm = jwt.property("realm").getString(),
                    expirationMillis = jwt.property("expirationMillis").getString().toLong(),
                ),
            )
        }
    }
}
