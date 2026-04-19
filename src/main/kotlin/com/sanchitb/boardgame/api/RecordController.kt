package com.sanchitb.boardgame.api

import com.sanchitb.boardgame.api.dto.RecordResponse
import com.sanchitb.boardgame.service.RecordService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/records")
class RecordController(
    private val records: RecordService,
) {

    /**
     * Body accepted as raw JSON text and parsed with our Jackson 2 mapper — the
     * schema validator (networknt) speaks Jackson 2, and we don't want Spring Boot
     * 4's Jackson 3 converter in the critical path for creation.
     */
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun create(@RequestBody body: String): ResponseEntity<RecordResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(records.create(body))

    @GetMapping
    fun list(
        @RequestParam(required = false) game: String?,
        @RequestParam(required = false, defaultValue = "100") limit: Int,
    ): List<RecordResponse> = records.list(game, limit)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): RecordResponse = records.findById(id)
}
