package com.sanchitb.boardgame.api.dto

import java.time.Instant
import java.util.UUID

data class SavedPlayerResponse(
    val id: UUID,
    val name: String,
    val email: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class SavedPlayerRequest(
    val name: String,
    val email: String? = null,
    val notes: String? = null,
)
