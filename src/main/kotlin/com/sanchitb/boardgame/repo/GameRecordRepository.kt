package com.sanchitb.boardgame.repo

import com.sanchitb.boardgame.domain.GameRecordEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface GameRecordRepository : JpaRepository<GameRecordEntity, UUID> {

    @Query("SELECT r FROM GameRecordEntity r ORDER BY r.createdAt DESC, r.date DESC")
    fun findAllNewest(pageable: Pageable): Page<GameRecordEntity>

    @Query(
        "SELECT r FROM GameRecordEntity r WHERE r.game = :game ORDER BY r.createdAt DESC, r.date DESC",
    )
    fun findByGame(game: String, pageable: Pageable): Page<GameRecordEntity>
}
