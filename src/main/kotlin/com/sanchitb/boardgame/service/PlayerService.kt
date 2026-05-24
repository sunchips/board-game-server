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

    /**
     * Look up a roster entry by case-insensitive name match, creating one if
     * it doesn't already exist. Called from the record-create path so
     * manually-entered players become roster members on first appearance.
     * Returns the resolved (id, name, ...) so the caller can stamp the id
     * onto the stored record.
     */
    @Transactional
    fun findOrCreateByName(userId: UUID, name: String, email: String?): SavedPlayerResponse {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "name must not be blank" }
        val existing = players.findFirstByUserIdAndNameIgnoreCase(userId, trimmed).orElse(null)
        if (existing != null) return existing.toResponse()
        return players.save(
            PlayerEntity(
                userId = userId,
                name = trimmed,
                email = email?.trim()?.takeIf { it.isNotEmpty() },
            ),
        ).toResponse()
    }

    /**
     * Ensure a roster entry exists for the user as themselves. Called from the
     * Apple sign-in path so a freshly-signed-up user can one-tap themselves
     * into a new record. Idempotent: returns the existing self row if present.
     */
    @Transactional
    fun ensureSelf(userId: UUID, displayName: String?, email: String?): SavedPlayerResponse {
        val existing = players.findByUserIdAndIsSelfTrue(userId).orElse(null)
        if (existing != null) return existing.toResponse()
        val name = displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: "Me"
        return players.save(
            PlayerEntity(
                userId = userId,
                name = name,
                email = email?.trim()?.takeIf { it.isNotEmpty() },
                isSelf = true,
            ),
        ).toResponse()
    }

    private fun PlayerEntity.toResponse() = SavedPlayerResponse(
        id = id,
        name = name,
        email = email,
        notes = notes,
        isSelf = isSelf,
        createdAt = createdAt ?: java.time.Instant.EPOCH,
        updatedAt = updatedAt ?: java.time.Instant.EPOCH,
    )
}
