package com.sanchitb.boardgame.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "game_records")
class GameRecordEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "game", nullable = false)
    var game: String = "",

    @Column(name = "year_published")
    var yearPublished: Int? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variants", nullable = false, columnDefinition = "jsonb")
    var variants: List<String> = emptyList(),

    @Column(name = "date", nullable = false)
    var date: LocalDate = LocalDate.now(),

    @Column(name = "player_count", nullable = false)
    var playerCount: Int = 0,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "winners", nullable = false, columnDefinition = "jsonb")
    var winners: List<Int> = emptyList(),

    @Column(name = "notes")
    var notes: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "players", nullable = false, columnDefinition = "jsonb")
    var players: List<Map<String, Any?>> = emptyList(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
)
