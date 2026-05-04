package com.example.warehouse_accounting_server.domain.service

import com.example.warehouse_accounting_server.config.ForbiddenException
import com.example.warehouse_accounting_server.config.NotFoundException
import com.example.warehouse_accounting_server.domain.model.User
import com.example.warehouse_accounting_server.domain.model.UserRole
import com.example.warehouse_accounting_server.domain.model.UserStatus
import com.example.warehouse_accounting_server.domain.repository.UserRepository
import com.example.warehouse_accounting_server.util.RoleAccess

/**
 * Проверки актуального пользователя в БД (не JWT).
 *
 * **Политика RBAC:** валидный JWT подтверждает только идентичность ([User.id] в claim `userId`).
 * Claim `role` в JWT — справочный для клиента; **иголка на доступ** — текущая запись в БД
 * (статус ACTIVE и фактическая роль), чтобы не доверять устаревшему токену после блокировки
 * или смены роли.
 */
class AccessControlService(
    private val userRepository: UserRepository,
) {
    fun requireActiveUser(userId: Long): User {
        val user =
            userRepository.findById(userId)
                ?: throw NotFoundException("Пользователь не найден")
        when (user.status) {
            UserStatus.ACTIVE -> return user
            UserStatus.BLOCKED -> throw ForbiddenException("Аккаунт заблокирован")
            UserStatus.PENDING -> throw ForbiddenException("Аккаунт ожидает подтверждения администратором")
        }
    }

    fun requireActiveAdmin(userId: Long): User {
        val user = requireActiveUser(userId)
        if (user.role != UserRole.ADMIN) {
            throw ForbiddenException("Недостаточно прав")
        }
        return user
    }

    fun requireAnyActiveRole(userId: Long, roles: Set<UserRole>): User {
        val user = requireActiveUser(userId)
        RoleAccess.require(user.role, *roles.toTypedArray())
        return user
    }

    /** Чтение складских данных: ADMIN, STOREKEEPER, MANAGER. */
    fun requireStockReader(userId: Long): User =
        requireAnyActiveRole(
            userId,
            setOf(UserRole.ADMIN, UserRole.STOREKEEPER, UserRole.MANAGER),
        )

    /** Складские операции (приход/расход/списание/инвентаризация): ADMIN, STOREKEEPER. */
    fun requireStockOperator(userId: Long): User =
        requireAnyActiveRole(userId, setOf(UserRole.ADMIN, UserRole.STOREKEEPER))

    /** Отчёты и аналитика (включая низкие остатки): ADMIN, MANAGER. */
    fun requireReportReader(userId: Long): User =
        requireAnyActiveRole(userId, setOf(UserRole.ADMIN, UserRole.MANAGER))
}
