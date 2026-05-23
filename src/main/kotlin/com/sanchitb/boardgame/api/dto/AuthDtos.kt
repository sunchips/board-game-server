package com.sanchitb.boardgame.api.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class AppleSignInRequest(
    @field:NotBlank
    val identityToken: String,
    val email: String? = null,
    val name: String? = null,
)

data class AuthResponse(
    val sessionToken: String,
    val expiresInSeconds: Long,
    val user: AuthUser,
)

data class AuthUser(
    val id: UUID,
    val email: String?,
    val name: String?,
    val createdAt: Instant,
)
