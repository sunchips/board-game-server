package com.sanchitb.boardgame.api

import com.sanchitb.boardgame.api.dto.AppleAuthRequest
import com.sanchitb.boardgame.api.dto.AuthResponse
import com.sanchitb.boardgame.api.dto.AuthUser
import com.sanchitb.boardgame.auth.AppleTokenVerifier
import com.sanchitb.boardgame.auth.JwtService
import com.sanchitb.boardgame.service.UserService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val apple: AppleTokenVerifier,
    private val jwt: JwtService,
    private val users: UserService,
) {

    /**
     * Exchanges an Apple ID token for a server session JWT. The caller is the
     * iOS app, which got the ID token from `ASAuthorizationAppleIDCredential`.
     */
    @PostMapping("/apple")
    fun exchangeApple(@RequestBody body: AppleAuthRequest): AuthResponse {
        val identity = apple.verify(body.identityToken)
        val user = users.upsertFromApple(identity, body.fullName)
        val issued = jwt.issue(user.id)
        return AuthResponse(
            token = issued.token,
            expiresAt = issued.expiresAt,
            user = AuthUser(id = user.id, email = user.email, displayName = user.displayName),
        )
    }
}
