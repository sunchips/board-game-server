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
    /** True when this is the first time we saw this Apple sub. */
    val isNewUser: Boolean,
)

data class AuthUser(
    val id: UUID,
    val email: String?,
    val displayName: String?,
)

/**
 * Bundle of everything the iOS app needs after authenticating — avoids a
 * fan-out of round-trips on launch. Returned from `GET /api/session`.
 */
data class SessionBundle(
    val user: AuthUser,
    val players: List<SavedPlayerResponse>,
    val records: List<RecordResponse>,
)
