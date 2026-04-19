package com.sanchitb.boardgame.api.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class RecordResponse(
    val id: UUID,
    val game: String,
    val yearPublished: Int?,
    val variants: List<String>,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val date: LocalDate,
    val playerCount: Int,
    val winners: List<Int>,
    val notes: String?,
    val players: List<PlayerResponse>,
    val createdAt: Instant,
)

data class PlayerResponse(
    val name: String,
    val email: String?,
    val identity: String?,
    val team: Int?,
    val eliminated: Boolean?,
    val endState: Map<String, Any>,
)
