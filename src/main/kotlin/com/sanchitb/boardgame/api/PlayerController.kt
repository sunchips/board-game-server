package com.sanchitb.boardgame.api

import com.sanchitb.boardgame.api.dto.SavedPlayerRequest
import com.sanchitb.boardgame.api.dto.SavedPlayerResponse
import com.sanchitb.boardgame.auth.userId
import com.sanchitb.boardgame.service.PlayerService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/players")
class PlayerController(private val service: PlayerService) {

    @GetMapping
    fun list(request: HttpServletRequest): List<SavedPlayerResponse> =
        service.list(request.userId())

    @PostMapping
    fun create(
        request: HttpServletRequest,
        @RequestBody body: SavedPlayerRequest,
    ): ResponseEntity<SavedPlayerResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.userId(), body))

    @PutMapping("/{id}")
    fun update(
        request: HttpServletRequest,
        @PathVariable id: UUID,
        @RequestBody body: SavedPlayerRequest,
    ): SavedPlayerResponse = service.update(request.userId(), id, body)

    @DeleteMapping("/{id}")
    fun delete(
        request: HttpServletRequest,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        service.delete(request.userId(), id)
        return ResponseEntity.noContent().build()
    }
}
