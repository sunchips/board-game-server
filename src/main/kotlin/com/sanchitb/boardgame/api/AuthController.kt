package com.sanchitb.boardgame.api

import com.sanchitb.boardgame.api.dto.AppleSignInRequest
import com.sanchitb.boardgame.api.dto.AuthResponse
import com.sanchitb.boardgame.api.dto.AuthUser
import com.sanchitb.boardgame.auth.AppleTokenVerifier
import com.sanchitb.boardgame.auth.JwtService
import com.sanchitb.boardgame.auth.UserService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val appleVerifier: AppleTokenVerifier,
    private val userService: UserService,
    private val jwtService: JwtService,
) {

    @PostMapping("/apple")
    fun signInWithApple(@Valid @RequestBody body: AppleSignInRequest): AuthResponse {
        val identity = appleVerifier.verify(body.identityToken)
        // Apple only sends email/name on the very first sign-in; prefer the verified
        // token claim over what the client sent, and fall back to the client-supplied
        // value (which is how the user's chosen display name reaches us).
        val email = identity.email ?: body.email
        val user = userService.findOrCreate(
            appleSub = identity.sub,
            email = email,
            name = body.name,
        )
        val token = jwtService.issue(userId = user.id, appleSub = user.appleSub)
        return AuthResponse(
            sessionToken = token,
            expiresInSeconds = jwtService.ttlSeconds,
            user = AuthUser(
                id = user.id,
                email = user.email,
                name = user.name,
                createdAt = user.createdAt ?: Instant.now(),
            ),
        )
    }
}
