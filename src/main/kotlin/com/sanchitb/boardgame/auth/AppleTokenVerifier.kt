package com.sanchitb.boardgame.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.sanchitb.boardgame.config.AuthProperties
import org.springframework.stereotype.Component
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

class InvalidAppleTokenException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

data class AppleIdentity(
    val sub: String,
    val email: String?,
)

@Component
class AppleTokenVerifier(props: AuthProperties) {

    private val expectedIssuer: String = props.apple.issuer
    private val expectedAudience: String = props.apple.clientId

    private val jwkProvider = JwkProviderBuilder(URI(props.apple.jwksUrl).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    fun verify(identityToken: String): AppleIdentity {
        val unverified: DecodedJWT = try {
            JWT.decode(identityToken)
        } catch (ex: Exception) {
            throw InvalidAppleTokenException("Identity token is not a valid JWT", ex)
        }
        val kid = unverified.keyId
            ?: throw InvalidAppleTokenException("Identity token header missing 'kid'")
        val publicKey = try {
            jwkProvider.get(kid).publicKey as RSAPublicKey
        } catch (ex: Exception) {
            throw InvalidAppleTokenException("Failed to resolve Apple signing key for kid=$kid", ex)
        }
        val algorithm = Algorithm.RSA256(publicKey, null)
        val verified: DecodedJWT = try {
            JWT.require(algorithm)
                .withIssuer(expectedIssuer)
                .withAudience(expectedAudience)
                .acceptLeeway(60)
                .build()
                .verify(identityToken)
        } catch (ex: JWTVerificationException) {
            throw InvalidAppleTokenException("Identity token verification failed: ${ex.message}", ex)
        }
        val sub = verified.subject
            ?: throw InvalidAppleTokenException("Identity token missing 'sub'")
        val email = verified.getClaim("email").takeUnless { it.isMissing || it.isNull }?.asString()
        return AppleIdentity(sub = sub, email = email)
    }
}
