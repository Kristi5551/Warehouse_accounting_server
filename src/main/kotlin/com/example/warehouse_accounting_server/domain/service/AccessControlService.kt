package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ForbiddenException
import com.example.warehouse_accounting_server.config.NotFoundException
import com.example.warehouse_accounting_server.domain.model.User
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.UserRepository

/**
 * Проверки актуального пользователя в БД (не JWT). Сервисы мутаций каталога обязаны вызывать
 * [requireActiveAdmin] для защиты от устаревшего токена.
 */
class AccessControlService(
    private val userRepository: UserRepository,
) {
    fun requireActiveUser(userId: Long): User {
        val user =
            userRepository.findById(userId)
                ?: throw NotFoundException("Пользователь не найден")
        if (user.status != UserStatus.ACTIVE) {
            throw ForbiddenException("Пользователь больше не активен")
        }
        return user
    }

    fun requireActiveAdmin(userId: Long): User {
        val user = requireActiveUser(userId)
        if (user.role != UserRole.ADMIN) {
            throw ForbiddenException("Недостаточно прав")
        }
        return user
    }
}
