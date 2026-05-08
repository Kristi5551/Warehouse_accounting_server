package com.example.warehouse_accounting_server.util

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.example.warehouse_accounting_server.config.JwtSettings
import com.example.warehouse_accounting_server.domain.model.User
import java.util.Date

class JwtProvider(
    private val settings: JwtSettings,
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(settings.secret)

    fun buildVerifier(): JWTVerifier =
        JWT.require(algorithm)
            .withAudience(settings.audience)
            .withIssuer(settings.issuer)
            .build()

    fun createAccessToken(user: User): String {
        val now = System.currentTimeMillis()
        val expiry = now + settings.expirationMillis
        return JWT.create()
            .withAudience(settings.audience)
            .withIssuer(settings.issuer)
            .withClaim(CLAIM_USER_ID, user.id)
            .withClaim(CLAIM_EMAIL, user.email)
            .withClaim(CLAIM_ROLE, user.role.name)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(expiry))
            .sign(algorithm)
    }

    companion object {
        const val CLAIM_USER_ID = "userId"
        const val CLAIM_EMAIL = "email"
        const val CLAIM_ROLE = "role"
    }
}
