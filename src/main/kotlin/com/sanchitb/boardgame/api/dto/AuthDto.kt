package com.sanchitb.boardgame.api.dto

import java.time.Instant
import java.util.UUID

data class AppleAuthRequest(
    /** The ID token returned by `ASAuthorizationAppleIDCredential.identityToken`. */
    val identityToken: String,
    /** Populated on first sign-in only; Apple doesn't return it on subsequent logins. */
    val fullName: String? = null,
)

data class AuthResponse(
    val token: String,
    val expiresAt: Instant,
    val user: AuthUser,
)

data class AuthUser(
    val id: UUID,
    val email: String?,
    val displayName: String?,
)
