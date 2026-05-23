package com.sanchitb.boardgame.service

import com.sanchitb.boardgame.api.dto.SavedPlayerRequest
import com.sanchitb.boardgame.api.dto.SavedPlayerResponse
import com.sanchitb.boardgame.domain.PlayerEntity
import com.sanchitb.boardgame.error.RecordNotFoundException
import com.sanchitb.boardgame.repo.PlayerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PlayerService(private val players: PlayerRepository) {

    @Transactional(readOnly = true)
    fun list(userId: UUID): List<SavedPlayerResponse> =
        players.findAllByUserIdOrderByNameAsc(userId).map { it.toResponse() }

    @Transactional
    fun create(userId: UUID, req: SavedPlayerRequest): SavedPlayerResponse {
        require(req.name.isNotBlank()) { "name must not be blank" }
        val saved = players.save(
            PlayerEntity(
                userId = userId,
                name = req.name.trim(),
                email = req.email?.trim()?.takeIf { it.isNotEmpty() },
                notes = req.notes?.takeIf { it.isNotBlank() },
            ),
        )
        return saved.toResponse()
    }

    @Transactional
    fun update(userId: UUID, id: UUID, req: SavedPlayerRequest): SavedPlayerResponse {
        require(req.name.isNotBlank()) { "name must not be blank" }
        val entity = players.findByIdAndUserId(id, userId)
            .orElseThrow { RecordNotFoundException("player: $id") }
        entity.name = req.name.trim()
        entity.email = req.email?.trim()?.takeIf { it.isNotEmpty() }
        entity.notes = req.notes?.takeIf { it.isNotBlank() }
        return players.save(entity).toResponse()
    }

    @Transactional
    fun delete(userId: UUID, id: UUID) {
        val deleted = players.deleteByIdAndUserId(id, userId)
        if (deleted == 0L) throw RecordNotFoundException("player: $id")
    }

    private fun PlayerEntity.toResponse() = SavedPlayerResponse(
        id = id,
        name = name,
        email = email,
        notes = notes,
        createdAt = createdAt ?: java.time.Instant.EPOCH,
        updatedAt = updatedAt ?: java.time.Instant.EPOCH,
    )
}
