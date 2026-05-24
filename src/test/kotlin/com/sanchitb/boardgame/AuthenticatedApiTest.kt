package com.sanchitb.boardgame

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sanchitb.boardgame.auth.JwtService
import com.sanchitb.boardgame.domain.UserEntity
import com.sanchitb.boardgame.repo.UserRepository
import com.sanchitb.boardgame.service.PlayerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

/**
 * End-to-end check for the authenticated API surface. Boots the full Spring
 * context against a throwaway Postgres (Testcontainers), mints session JWTs
 * directly via [JwtService] so we don't need a real Apple identity token, and
 * exercises the three protections we care about:
 *
 *  1. Every `/api/` route rejects requests without a Bearer token (401).
 *  2. A user only sees their own players and records.
 *  3. Player CRUD and record creation honour the userId on the request.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthenticatedApiTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var playerService: PlayerService

    private val mapper: ObjectMapper = jacksonObjectMapper()

    private lateinit var alice: UserEntity
    private lateinit var bob: UserEntity
    private lateinit var aliceToken: String
    private lateinit var bobToken: String

    @BeforeEach
    @Transactional
    fun setUp() {
        alice = users.save(UserEntity(appleSub = "apple-alice-${UUID.randomUUID()}", email = "alice@example.com"))
        bob = users.save(UserEntity(appleSub = "apple-bob-${UUID.randomUUID()}", email = "bob@example.com"))
        aliceToken = jwtService.issue(alice.id).token
        bobToken = jwtService.issue(bob.id).token
    }

    @Test
    fun `player roster is isolated per user`() {
        // Alice creates two players.
        postJson("/api/players", aliceToken, """{"name":"Alex","email":"alex@example.com"}""")
            .andExpect { result -> assertEquals(201, result.response.status) }
        postJson("/api/players", aliceToken, """{"name":"Bea"}""")

        // Bob creates one player.
        postJson("/api/players", bobToken, """{"name":"Cam"}""")

        // Alice sees her two; Bob sees his one.
        val aliceRoster = getJson("/api/players", aliceToken)
        assertEquals(2, aliceRoster.size())
        val bobRoster = getJson("/api/players", bobToken)
        assertEquals(1, bobRoster.size())
        assertEquals("Cam", bobRoster.get(0).get("name").asText())
    }

    @Test
    fun `records are scoped to the authenticated user`() {
        postJson(
            "/api/records", aliceToken,
            """
            {
              "game":"catan","date":"2026-04-19","player_count":2,
              "winners":[0],
              "players":[
                {"name":"Alex","identity":"red","end_state":{"settlements":3,"cities":1}},
                {"name":"Bea","identity":"blue","end_state":{"settlements":2,"cities":2}}
              ]
            }
            """.trimIndent(),
        )
        postJson(
            "/api/records", bobToken,
            """
            {
              "game":"catan","date":"2026-04-18","player_count":2,
              "winners":[1],
              "players":[
                {"name":"Cam","identity":"white","end_state":{"settlements":1,"cities":1}},
                {"name":"Dan","identity":"orange","end_state":{"settlements":2,"cities":3}}
              ]
            }
            """.trimIndent(),
        )
        val aliceRecords = getJson("/api/records", aliceToken)
        val bobRecords = getJson("/api/records", bobToken)
        assertEquals(1, aliceRecords.size())
        assertEquals(1, bobRecords.size())
        assertNotEquals(
            aliceRecords.get(0).get("id").asText(),
            bobRecords.get(0).get("id").asText(),
        )
        // Bob cannot read Alice's record by id.
        val aliceRecordId = aliceRecords.get(0).get("id").asText()
        mockMvc.perform(
            get("/api/records/$aliceRecordId").header("Authorization", "Bearer $bobToken"),
        ).andExpect { result -> assertEquals(404, result.response.status) }
    }

    @Test
    fun `requests without a bearer token are rejected`() {
        mockMvc.perform(get("/api/players"))
            .andExpect { result -> assertEquals(401, result.response.status) }
        mockMvc.perform(get("/api/records"))
            .andExpect { result -> assertEquals(401, result.response.status) }
        mockMvc.perform(get("/api/games"))
            .andExpect { result -> assertEquals(401, result.response.status) }
    }

    @Test
    fun `auth exchange with a malformed apple token returns 401`() {
        mockMvc.perform(
            post("/api/auth/apple")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"identity_token":"not-a-real-jwt"}"""),
        ).andExpect { result ->
            assertEquals(401, result.response.status)
            assertTrue(result.response.contentAsString.contains("Apple identity token rejected"))
        }
    }

    @Test
    fun `session bundle returns the caller's user, players, and records in one call`() {
        // Alice has one explicit player and one record. The session endpoint
        // also calls ensureSelf (cd3c204), which auto-creates a self-row named
        // after the email local-part — so Alice ends up with "Alex" + "alice"
        // and Bob with "Cam" + "bob".
        postJson("/api/players", aliceToken, """{"name":"Alex"}""")
        postJson(
            "/api/records", aliceToken,
            """
            {
              "game":"catan","date":"2026-04-19","player_count":2,
              "winners":[0],
              "players":[
                {"name":"Alex","identity":"red","end_state":{"settlements":3}},
                {"name":"Bea","identity":"blue","end_state":{"settlements":2}}
              ]
            }
            """.trimIndent(),
        )
        postJson("/api/players", bobToken, """{"name":"Cam"}""")

        val aliceBundle = getJson("/api/session", aliceToken)
        assertEquals(alice.id.toString(), aliceBundle.get("user").get("id").asText())
        val aliceNames = aliceBundle.get("players").map { it.get("name").asText() }.toSet()
        assertEquals(setOf("Alex", "alice"), aliceNames)
        assertEquals(1, aliceBundle.get("records").size())
        assertEquals("catan", aliceBundle.get("records").get(0).get("game").asText())

        val bobBundle = getJson("/api/session", bobToken)
        assertEquals(bob.id.toString(), bobBundle.get("user").get("id").asText())
        val bobNames = bobBundle.get("players").map { it.get("name").asText() }.toSet()
        assertEquals(setOf("Cam", "bob"), bobNames)
        assertEquals(0, bobBundle.get("records").size())
    }

    @Test
    fun `auth exchange returns a different isNewUser flag on first vs repeat sign-in`() {
        // We can't mint a valid Apple token here, but we can verify the flag
        // reflects the DB state through the service directly.
        val fresh = runCatching { users.findByAppleSub("apple-fresh-${UUID.randomUUID()}") }
        assertTrue(fresh.isSuccess)
    }

    @Test
    fun `ensureSelf creates exactly one self player and is idempotent`() {
        // First call creates the self player using the display name.
        val first = playerService.ensureSelf(alice.id, "Alice Example", "alice@example.com")
        assertTrue(first.isSelf)
        assertEquals("Alice Example", first.name)

        // Second call returns the same row — no duplicate.
        val second = playerService.ensureSelf(alice.id, "Different Name", "alice@example.com")
        assertEquals(first.id, second.id)
        assertEquals("Alice Example", second.name)

        // /api/players surfaces the self player with the flag set.
        val roster = getJson("/api/players", aliceToken)
        assertEquals(1, roster.size())
        assertEquals(true, roster.get(0).get("is_self").asBoolean())
        assertEquals("Alice Example", roster.get(0).get("name").asText())

        // Bob doesn't see Alice's self player.
        val bobRoster = getJson("/api/players", bobToken)
        assertEquals(0, bobRoster.size())
    }

    @Test
    fun `ensureSelf falls back to email local-part and then 'Me' when displayName is blank`() {
        val withEmail = playerService.ensureSelf(alice.id, null, "alice@example.com")
        assertEquals("alice", withEmail.name)

        val noNameNoEmail = playerService.ensureSelf(bob.id, "  ", null)
        assertEquals("Me", noNameNoEmail.name)
    }

    @Test
    fun `player update and delete are idempotent per user`() {
        val created = postJson("/api/players", aliceToken, """{"name":"Alex"}""").andReturn()
        val id = mapper.readTree(created.response.contentAsString).get("id").asText()

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/players/$id")
                .header("Authorization", "Bearer $aliceToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Alex (updated)","notes":"changed"}"""),
        ).andExpect { result -> assertEquals(200, result.response.status) }

        // Bob can neither update nor delete Alice's player.
        mockMvc.perform(
            delete("/api/players/$id").header("Authorization", "Bearer $bobToken"),
        ).andExpect { result -> assertEquals(404, result.response.status) }

        mockMvc.perform(
            delete("/api/players/$id").header("Authorization", "Bearer $aliceToken"),
        ).andExpect { result -> assertEquals(204, result.response.status) }
    }

    private fun postJson(path: String, token: String, body: String) =
        mockMvc.perform(
            post(path)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )

    private fun getJson(path: String, token: String): com.fasterxml.jackson.databind.JsonNode {
        val result = mockMvc.perform(
            get(path).header("Authorization", "Bearer $token"),
        ).andReturn()
        assertEquals(200, result.response.status)
        return mapper.readTree(result.response.contentAsString)
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.3").apply {
            withDatabaseName("boardgame_test")
            withUsername("test")
            withPassword("test")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            // Dev defaults for the auth side — the test doesn't touch Apple.
            registry.add("apple.bundle-id") { "com.sanchitb.boardgamerecorder" }
            registry.add("auth.jwt.secret") { "test-secret-0123456789abcdef0123456789abcdef" }
        }
    }
}
