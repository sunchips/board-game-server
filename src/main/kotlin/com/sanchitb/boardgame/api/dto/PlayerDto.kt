package com.sanchitb.boardgame.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

data class SavedPlayerResponse(
    val id: UUID,
    val name: String,
    val email: String?,
    val notes: String?,
    @JsonProperty("is_self") val isSelf: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class SavedPlayerRequest(
    val name: String,
    val email: String? = null,
    val notes: String? = null,
)
