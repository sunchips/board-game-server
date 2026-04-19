package com.sanchitb.boardgame.auth

import com.auth0.jwk.GuavaCachedJwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.RSAKeyProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URL
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

/**
 * Verifies Apple Sign-In ID tokens. Apple signs RS256 JWTs with a rotating set
 * of keys published at https://appleid.apple.com/auth/keys. We pick the key by
 * `kid`, verify the signature, and check that `iss`, `aud`, and `exp` match.
 *
 * Returns [AppleIdentity] on success or throws [InvalidAppleTokenException].
 */
@Component
class AppleTokenVerifier(
    @Value("\${apple.bundle-id}") private val expectedAudience: String,
    @Value("\${apple.keys-url:https://appleid.apple.com/auth/keys}") private val keysUrl: String,
    @Value("\${apple.issuer:https://appleid.apple.com}") private val expectedIssuer: String,
) {

    private val keyProvider = GuavaCachedJwkProvider(
        JwkProviderBuilder(URL(keysUrl))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build(),
    )

    private val rsaKeyProvider = object : RSAKeyProvider {
        override fun getPublicKeyById(keyId: String): RSAPublicKey =
            keyProvider.get(keyId).publicKey as RSAPublicKey
        override fun getPrivateKey(): RSAPrivateKey? = null
        override fun getPrivateKeyId(): String? = null
    }

    fun verify(identityToken: String): AppleIdentity {
        val decoded: DecodedJWT = try {
            JWT.require(Algorithm.RSA256(rsaKeyProvider))
                .withIssuer(expectedIssuer)
                .withAudience(expectedAudience)
                .build()
                .verify(identityToken)
        } catch (e: Exception) {
            throw InvalidAppleTokenException("Apple identity token rejected: ${e.message}", e)
        }
        val sub = decoded.subject
            ?: throw InvalidAppleTokenException("Apple token missing `sub` claim")
        return AppleIdentity(
            sub = sub,
            email = decoded.getClaim("email").asString().takeUnless { it.isNullOrBlank() },
            emailVerified = decoded.getClaim("email_verified").asBoolean() ?: false,
        )
    }
}

data class AppleIdentity(
    val sub: String,
    val email: String?,
    val emailVerified: Boolean,
)

class InvalidAppleTokenException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
