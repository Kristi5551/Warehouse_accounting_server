package com.example.warehouse_accounting_server.config

import com.example.warehouse_accounting_server.data.table.UsersTable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Проверка доступности БД: выполняется лёгкий запрос к существующей таблице (эквивалент проверки соединения).
 */
object HealthService {
    fun pingDatabase() {
        transaction {
            UsersTable.select(UsersTable.id).limit(1).forEach { _ -> }
        }
    }
}
