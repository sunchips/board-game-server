package com.sanchitb.boardgame.api.dto

data class GameSummary(
    val slug: String,
    val displayName: String,
    val yearPublished: Int?,
    val identityOptions: List<String>,
    val endStateFields: List<EndStateFieldSpec>,
    val supportsTeams: Boolean,
    val supportsElimination: Boolean,
    val variants: List<String>,
)

data class EndStateFieldSpec(
    val key: String,
    val type: String,
    val min: Int?,
    val max: Int?,
)
