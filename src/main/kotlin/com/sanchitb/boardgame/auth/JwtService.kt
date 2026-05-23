package com.sanchitb.boardgame.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.sanchitb.boardgame.config.AuthProperties
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import java.util.UUID

class InvalidSessionTokenException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

data class SessionPrincipal(
    val userId: UUID,
    val appleSub: String,
)

@Component
class JwtService(private val props: AuthProperties) {

    private val algorithm: Algorithm = Algorithm.HMAC256(props.jwtSecret)
    private val verifier = JWT.require(algorithm).withIssuer(props.jwtIssuer).build()

    fun issue(userId: UUID, appleSub: String, now: Instant = Instant.now()): String {
        val expiresAt = now.plusSeconds(props.jwtTtlSeconds)
        return JWT.create()
            .withIssuer(props.jwtIssuer)
            .withSubject(userId.toString())
            .withClaim(CLAIM_APPLE_SUB, appleSub)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)
    }

    fun parse(token: String): SessionPrincipal {
        val decoded = try {
            verifier.verify(token)
        } catch (ex: JWTVerificationException) {
            throw InvalidSessionTokenException("Session token invalid: ${ex.message}", ex)
        }
        val sub = decoded.subject
            ?: throw InvalidSessionTokenException("Session token missing 'sub'")
        val userId = try {
            UUID.fromString(sub)
        } catch (ex: IllegalArgumentException) {
            throw InvalidSessionTokenException("Session token 'sub' is not a UUID", ex)
        }
        val appleSub = decoded.getClaim(CLAIM_APPLE_SUB).takeUnless { it.isMissing || it.isNull }?.asString()
            ?: throw InvalidSessionTokenException("Session token missing '$CLAIM_APPLE_SUB' claim")
        return SessionPrincipal(userId = userId, appleSub = appleSub)
    }

    val ttlSeconds: Long get() = props.jwtTtlSeconds

    private companion object {
        const val CLAIM_APPLE_SUB = "apple_sub"
    }
}
