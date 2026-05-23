package com.sanchitb.boardgame.auth

import com.sanchitb.boardgame.config.AuthProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class JwtServiceTest {

    private val props = AuthProperties(
        jwtSecret = "test-secret-test-secret-test-secret",
        jwtTtlSeconds = 3600,
        jwtIssuer = "test-issuer",
        apple = AuthProperties.Apple(
            clientId = "com.example.app",
            issuer = "https://appleid.apple.com",
            jwksUrl = "https://appleid.apple.com/auth/keys",
        ),
    )
    private val service = JwtService(props)

    @Test
    fun `issue and parse roundtrips user id and apple sub`() {
        val userId = UUID.randomUUID()
        val token = service.issue(userId = userId, appleSub = "001234.abcdef.5678")
        val principal = service.parse(token)
        assertEquals(userId, principal.userId)
        assertEquals("001234.abcdef.5678", principal.appleSub)
    }

    @Test
    fun `tampered token is rejected`() {
        val token = service.issue(userId = UUID.randomUUID(), appleSub = "001234.abcdef.5678")
        val tampered = token.dropLast(4) + "AAAA"
        assertThrows(InvalidSessionTokenException::class.java) { service.parse(tampered) }
    }

    @Test
    fun `expired token is rejected`() {
        val past = Instant.now().minusSeconds(props.jwtTtlSeconds + 120)
        val token = service.issue(userId = UUID.randomUUID(), appleSub = "001234.abcdef.5678", now = past)
        assertThrows(InvalidSessionTokenException::class.java) { service.parse(token) }
    }
}
