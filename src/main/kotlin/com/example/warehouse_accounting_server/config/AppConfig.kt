package com.example.warehouse_accounting_server.config

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import java.util.Locale

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
        /**
         * Строка по умолчанию в файле `application.conf` для `warehouse.jwt.secret` при отсутствии `JWT_SECRET` в окружении.
         * Не использовать как боевой секрет; для production/staging задайте **`JWT_SECRET` только через env**.
         */
        const val LOCAL_JWT_SECRET_FALLBACK = "change-this-secret-for-local-development"

        fun load(app: Application): AppConfig {
            return from(app.environment.config)
        }

        fun from(config: ApplicationConfig): AppConfig {
            validateJwtSecretForNonLocalDeployments()
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

        /**
         * Для окружений **production** / **staging** процесс обязан явно получить секрет из **`JWT_SECRET`**;
         * fallback из конфига допустим только для локальной разработки и учебного запуска.
         */
        private fun validateJwtSecretForNonLocalDeployments() {
            val appEnv = System.getenv("APP_ENV")?.trim()?.lowercase(Locale.ROOT).orEmpty()
            if (appEnv.isEmpty()) return
            val requiresEnvSecret =
                appEnv == "production" ||
                    appEnv == "prod" ||
                    appEnv == "staging"
            if (!requiresEnvSecret) return

            val fromEnv = System.getenv("JWT_SECRET")?.trim().orEmpty()
            if (fromEnv.isNotEmpty()) return

            throw IllegalStateException(
                "APP_ENV=$appEnv: задайте JWT_SECRET в переменных окружения процесса. " +
                    "Значение по умолчанию в application.conf предназначено только для локальной разработки и не является боевым секретом.",
            )
        }
    }
}
