package com.sanchitb.boardgame.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Issues and parses the server's own session JWTs. These are HS256-signed so
 * we can verify without a key lookup round-trip on every request.
 *
 * The `userId` claim carries the primary-key of the [com.sanchitb.boardgame.domain.UserEntity]
 * row; filters resolve it on each request.
 */
@Service
class JwtService(
    @Value("\${auth.jwt.secret}") private val secret: String,
    @Value("\${auth.jwt.ttl-hours:720}") ttlHours: Long,
    @Value("\${auth.jwt.issuer:board-game-server}") private val issuer: String,
) {
    private val ttl: Duration = Duration.ofHours(ttlHours)
    private val algorithm by lazy { Algorithm.HMAC256(secret) }

    fun issue(userId: UUID, now: Instant = Instant.now()): IssuedToken {
        val expiresAt = now.plus(ttl)
        val token = JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.toString())
            .withClaim("userId", userId.toString())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)
        return IssuedToken(token = token, expiresAt = expiresAt)
    }

    fun parse(token: String): UUID {
        val decoded = try {
            JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token)
        } catch (e: JWTVerificationException) {
            throw InvalidSessionTokenException("session token rejected: ${e.message}", e)
        }
        val claim = decoded.getClaim("userId").asString()
            ?: decoded.subject
            ?: throw InvalidSessionTokenException("session token missing userId/sub")
        return try {
            UUID.fromString(claim)
        } catch (e: IllegalArgumentException) {
            throw InvalidSessionTokenException("session token has non-UUID userId", e)
        }
    }
}

data class IssuedToken(val token: String, val expiresAt: Instant)

class InvalidSessionTokenException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
