package com.sanchitb.boardgame.error

data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val violations: List<Violation> = emptyList(),
)

data class Violation(
    val path: String,
    val message: String,
)

class SchemaValidationException(
    message: String,
    val violations: List<Violation>,
) : RuntimeException(message)

class GameNotFoundException(slug: String, yearPublished: Int?) : RuntimeException(
    "No game schema found for slug='$slug'" + (yearPublished?.let { ", year=$it" } ?: ""),
)

class RecordNotFoundException(id: String) : RuntimeException("Record not found: $id")
