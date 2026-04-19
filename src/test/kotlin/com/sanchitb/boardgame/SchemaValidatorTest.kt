package com.sanchitb.boardgame

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sanchitb.boardgame.error.SchemaValidationException
import com.sanchitb.boardgame.service.GameCatalogService
import com.sanchitb.boardgame.service.SchemaValidator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaValidatorTest {

    private val mapper: ObjectMapper = jacksonObjectMapper()
    private lateinit var catalog: GameCatalogService
    private lateinit var validator: SchemaValidator

    @BeforeAll
    fun setUp() {
        // These services load schemas from classpath; Gradle's copy task stages them
        // at ../board-game-record -> build/generated-resources/schemas before tests run.
        catalog = GameCatalogService(mapper).also { it.init() }
        validator = SchemaValidator(mapper, catalog).also { it.init() }
    }

    @Test
    fun `catalog lists every game folder`() {
        val slugs = catalog.allGames().map { it.slug }.toSet()
        val expected = setOf(
            "bunny-kingdom", "calico", "catan", "codenames", "coup",
            "everdell", "hues-and-cues", "jaipur", "king-of-new-york",
            "parks", "scythe", "secret-hitler", "the-king-is-dead",
            "viticulture", "wavelength",
        )
        assertEquals(expected, slugs)
    }

    @Test
    fun `every checked-in example record validates`() {
        val base = Path.of("..", "board-game-record", "games")
        if (!Files.isDirectory(base)) return
        val examples = Files.walk(base).use { stream ->
            stream.filter { it.fileName.toString() == "example.json" }.toList()
        }
        assertTrue(examples.isNotEmpty(), "expected at least one example.json")
        for (path in examples) {
            val node = mapper.readTree(path.toFile())
            validator.validate(node) // throws on failure
        }
    }

    @Test
    fun `mismatched player_count fails validation`() {
        val body = mapper.readTree(
            """
            {
              "game": "catan",
              "date": "2026-04-19",
              "player_count": 2,
              "winners": [0],
              "players": [
                {"name": "A", "end_state": {"settlements": 3}}
              ]
            }
            """.trimIndent(),
        )
        val ex = assertThrows(SchemaValidationException::class.java) { validator.validate(body) }
        assertTrue(ex.violations.any { it.path == "/player_count" }, "expected player_count violation")
    }

    @Test
    fun `unknown game slug fails with pointer to game field`() {
        val body = mapper.readTree(
            """
            {
              "game": "not-a-real-game",
              "date": "2026-04-19",
              "player_count": 1,
              "winners": [0],
              "players": [{"name": "A", "end_state": {}}]
            }
            """.trimIndent(),
        )
        val ex = assertThrows(SchemaValidationException::class.java) { validator.validate(body) }
        assertTrue(ex.violations.any { it.path == "/game" })
    }

    @Test
    fun `winner index out of range fails`() {
        val body = mapper.readTree(
            """
            {
              "game": "catan",
              "date": "2026-04-19",
              "player_count": 1,
              "winners": [5],
              "players": [{"name": "A", "identity": "red", "end_state": {"settlements": 1}}]
            }
            """.trimIndent(),
        )
        val ex = assertThrows(SchemaValidationException::class.java) { validator.validate(body) }
        assertTrue(ex.violations.any { it.path == "/winners/0" })
    }
}
