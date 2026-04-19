package com.sanchitb.boardgame.api.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class CreateRecordRequest(
    val game: String,
    val variants: List<String> = emptyList(),
    val yearPublished: Int? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val date: LocalDate,
    val playerCount: Int,
    val winners: List<Int>,
    val notes: String? = null,
    val players: List<PlayerDto>,
)

data class PlayerDto(
    val name: String,
    val email: String? = null,
    val identity: String? = null,
    val team: Int? = null,
    val eliminated: Boolean? = null,
    val endState: Map<String, Any> = emptyMap(),
)
