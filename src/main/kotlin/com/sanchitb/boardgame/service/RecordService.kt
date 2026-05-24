package com.sanchitb.boardgame.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sanchitb.boardgame.api.dto.CreateRecordRequest
import com.sanchitb.boardgame.api.dto.PlayerResponse
import com.sanchitb.boardgame.api.dto.RecordResponse
import com.sanchitb.boardgame.domain.GameRecordEntity
import com.sanchitb.boardgame.error.RecordNotFoundException
import com.sanchitb.boardgame.repo.GameRecordRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RecordService(
    private val repo: GameRecordRepository,
    private val schemaValidator: SchemaValidator,
    private val players: PlayerService,
    @Qualifier("legacyObjectMapper")
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun create(userId: UUID, body: String): RecordResponse {
        val req = parseAndValidate(body)
        val entity = GameRecordEntity(userId = userId)
        applyTo(entity, userId, req)
        return repo.save(entity).toResponse()
    }

    @Transactional
    fun update(userId: UUID, id: UUID, body: String): RecordResponse {
        val entity = repo.findByIdAndUserId(id, userId)
            .orElseThrow { RecordNotFoundException(id.toString()) }
        val req = parseAndValidate(body)
        applyTo(entity, userId, req)
        return repo.save(entity).toResponse()
    }

    private fun parseAndValidate(body: String): CreateRecordRequest {
        val jsonNode = objectMapper.readTree(body)
        schemaValidator.validate(jsonNode)
        return objectMapper.treeToValue(jsonNode, CreateRecordRequest::class.java)
    }

    /// Mutate [entity] from [req]. Shared by create + update so the find-or-create
    /// roster logic, snake_case key mapping, and field list stay in lockstep.
    private fun applyTo(entity: GameRecordEntity, userId: UUID, req: CreateRecordRequest) {
        entity.game = req.game
        entity.yearPublished = req.yearPublished
        entity.variants = req.variants
        entity.date = req.date
        entity.playerCount = req.playerCount
        entity.winners = req.winners
        entity.notes = req.notes
        entity.players = req.players.map { p ->
            // Players without an explicit saved_player_id get find-or-created
            // in the roster so manual additions grow the user's player list
            // naturally. The resolved id is stamped onto the stored player
            // JSON so future record reads can link back to the roster row.
            val resolvedId = p.savedPlayerId
                ?: players.findOrCreateByName(userId, p.name, p.email).id
            buildMap {
                put("name", p.name)
                p.email?.let { put("email", it) }
                p.identity?.let { put("identity", it) }
                p.team?.let { put("team", it) }
                p.eliminated?.let { put("eliminated", it) }
                put("saved_player_id", resolvedId.toString())
                put("end_state", p.endState)
            }
        }
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID, game: String?, limit: Int): List<RecordResponse> {
        val pageable = PageRequest.of(0, limit.coerceIn(1, 500))
        val page = if (game.isNullOrBlank()) {
            repo.findAllForUser(userId, pageable)
        } else {
            repo.findByUserAndGame(userId, game, pageable)
        }
        return page.content.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findById(userId: UUID, id: UUID): RecordResponse =
        repo.findByIdAndUserId(id, userId)
            .orElseThrow { RecordNotFoundException(id.toString()) }
            .toResponse()

    @Transactional
    fun delete(userId: UUID, id: UUID) {
        val deleted = repo.deleteByIdAndUserId(id, userId)
        if (deleted == 0L) throw RecordNotFoundException(id.toString())
    }

    private fun GameRecordEntity.toResponse(): RecordResponse = RecordResponse(
        id = this.id,
        game = this.game,
        yearPublished = this.yearPublished,
        variants = this.variants,
        date = this.date,
        playerCount = this.playerCount,
        winners = this.winners,
        notes = this.notes,
        players = this.players.map { it.toPlayerResponse() },
        createdAt = this.createdAt ?: java.time.Instant.EPOCH,
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.toPlayerResponse(): PlayerResponse = PlayerResponse(
        name = this["name"] as String,
        email = this["email"] as String?,
        identity = this["identity"] as String?,
        team = (this["team"] as Number?)?.toInt(),
        eliminated = this["eliminated"] as Boolean?,
        endState = (this["end_state"] as Map<String, Any>?).orEmpty(),
        savedPlayerId = (this["saved_player_id"] as String?)?.let { runCatching { UUID.fromString(it) }.getOrNull() },
    )
}
