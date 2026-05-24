package com.sanchitb.boardgame.repo

import com.sanchitb.boardgame.domain.GameRecordEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface GameRecordRepository : JpaRepository<GameRecordEntity, UUID> {

    @Query(
        "SELECT r FROM GameRecordEntity r WHERE r.userId = :userId ORDER BY r.createdAt DESC, r.date DESC",
    )
    fun findAllForUser(userId: UUID, pageable: Pageable): Page<GameRecordEntity>

    @Query(
        "SELECT r FROM GameRecordEntity r WHERE r.userId = :userId AND r.game = :game ORDER BY r.createdAt DESC, r.date DESC",
    )
    fun findByUserAndGame(userId: UUID, game: String, pageable: Pageable): Page<GameRecordEntity>

    fun findByIdAndUserId(id: UUID, userId: UUID): Optional<GameRecordEntity>

    /**
     * Per-user delete that doubles as the ownership check — returns 0 when the
     * row doesn't exist OR belongs to another user. Service layer turns that
     * into a 404 so foreign-user deletes leak no information.
     */
    fun deleteByIdAndUserId(id: UUID, userId: UUID): Long
}
