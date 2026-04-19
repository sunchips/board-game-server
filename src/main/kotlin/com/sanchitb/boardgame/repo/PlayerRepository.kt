package com.sanchitb.boardgame.repo

import com.sanchitb.boardgame.domain.PlayerEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface PlayerRepository : JpaRepository<PlayerEntity, UUID> {
    fun findAllByUserIdOrderByNameAsc(userId: UUID): List<PlayerEntity>
    fun findByIdAndUserId(id: UUID, userId: UUID): Optional<PlayerEntity>
    fun deleteByIdAndUserId(id: UUID, userId: UUID): Long
}
