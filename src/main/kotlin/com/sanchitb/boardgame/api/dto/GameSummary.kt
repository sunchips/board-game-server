package com.sanchitb.boardgame.api.dto

data class GameSummary(
    val slug: String,
    val displayName: String,
    val yearPublished: Int?,
    val identityOptions: List<String>,
    val endStateFields: List<EndStateFieldSpec>,
    val supportsTeams: Boolean,
    val supportsElimination: Boolean,
    /**
     * Fully cooperative game (e.g. Hanabi, Pandemic). When true, `winners` on a
     * record is conventionally all-or-nothing: either every player index is
     * listed (team win) or the array is empty (team loss). Clients should
     * surface a single team-won toggle rather than a per-player picker.
     */
    val isCooperative: Boolean,
    val variants: List<String>,
)

data class EndStateFieldSpec(
    val key: String,
    val type: String,
    val min: Int?,
    val max: Int?,
)
