package com.sanchitb.boardgame.api

import com.sanchitb.boardgame.api.dto.GameSummary
import com.sanchitb.boardgame.service.GameCatalogService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/games")
class GameController(
    private val catalog: GameCatalogService,
) {

    @GetMapping
    fun list(): List<GameSummary> = catalog.allGames()

    @GetMapping("/{slug}")
    fun get(@PathVariable slug: String): GameSummary = catalog.gameSummary(slug)
}
