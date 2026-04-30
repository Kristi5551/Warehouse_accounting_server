package com.example.warehouse_accounting_server.util

import at.favre.lib.crypto.bcrypt.BCrypt

class PasswordHasher(
    private val cost: Int = 10,
) {
    fun hash(raw: String): String =
        BCrypt.withDefaults().hashToString(cost, raw.toCharArray())

    fun verify(raw: String, hash: String): Boolean =
        BCrypt.verifyer().verify(raw.toCharArray(), hash).verified
}
