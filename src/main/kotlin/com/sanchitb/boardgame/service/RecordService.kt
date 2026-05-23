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
    @Qualifier("legacyObjectMapper")
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun create(userId: UUID, body: String): RecordResponse {
        val jsonNode = objectMapper.readTree(body)
        schemaValidator.validate(jsonNode)
        val req = objectMapper.treeToValue(jsonNode, CreateRecordRequest::class.java)
        val entity = GameRecordEntity(
            userId = userId,
            game = req.game,
            yearPublished = req.yearPublished,
            variants = req.variants,
            date = req.date,
            playerCount = req.playerCount,
            winners = req.winners,
            notes = req.notes,
            players = req.players.map { p ->
                buildMap {
                    put("name", p.name)
                    p.email?.let { put("email", it) }
                    p.identity?.let { put("identity", it) }
                    p.team?.let { put("team", it) }
                    p.eliminated?.let { put("eliminated", it) }
                    put("end_state", p.endState)
                }
            },
        )
        return repo.save(entity).toResponse()
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
    )
}
