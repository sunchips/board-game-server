package com.sanchitb.boardgame.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sanchitb.boardgame.api.dto.EndStateFieldSpec
import com.sanchitb.boardgame.api.dto.GameSummary
import com.sanchitb.boardgame.error.GameNotFoundException
import jakarta.annotation.PostConstruct
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

@Service
class GameCatalogService(
    private val objectMapper: ObjectMapper,
) {

    private lateinit var games: Map<GameKey, LoadedGame>
    private lateinit var summaries: List<GameSummary>

    private data class GameKey(val slug: String, val year: Int?)

    private data class LoadedGame(
        val slug: String,
        val yearPublished: Int?,
        val baseSchema: JsonNode,
        val folderPath: String,
        val variantSchemas: Map<String, JsonNode>,
    )

    @PostConstruct
    fun init() {
        val resolver = PathMatchingResourcePatternResolver(this::class.java.classLoader)
        val resources = resolver.getResources("classpath*:schemas/games/**/*.schema.json")

        val folderGroups = resources.groupBy { resource ->
            val url = resource.url.toString()
            val marker = "schemas/games/"
            val start = url.indexOf(marker) + marker.length
            val rest = url.substring(start)
            rest.substringBefore('/')
        }

        val loaded = mutableMapOf<GameKey, LoadedGame>()
        for ((folderName, files) in folderGroups) {
            val (slug, year) = parseFolder(folderName)
            val baseFile = files.firstOrNull { it.filename == "$slug.schema.json" }
                ?: continue
            val base = objectMapper.readTree(
                baseFile.inputStream.readBytes().toString(StandardCharsets.UTF_8),
            )
            val variants = files
                .filter { it.filename != "$slug.schema.json" }
                .associate {
                    val name = it.filename!!.removeSuffix(".schema.json")
                    name to objectMapper.readTree(
                        it.inputStream.readBytes().toString(StandardCharsets.UTF_8),
                    )
                }
            loaded[GameKey(slug, year)] = LoadedGame(
                slug = slug,
                yearPublished = year,
                baseSchema = base,
                folderPath = folderName,
                variantSchemas = variants,
            )
        }
        games = loaded
        summaries = loaded.values
            .map { buildSummary(it) }
            .sortedBy { it.displayName }
    }

    fun allGames(): List<GameSummary> = summaries

    fun gameSummary(slug: String): GameSummary =
        summaries.firstOrNull { it.slug == slug }
            ?: throw GameNotFoundException(slug, null)

    fun resolveGame(slug: String, yearPublished: Int?): LoadedGameView {
        val exact = games[GameKey(slug, null)]
        if (exact != null) return LoadedGameView(exact)
        if (yearPublished != null) {
            val tagged = games[GameKey(slug, yearPublished)]
            if (tagged != null) return LoadedGameView(tagged)
        }
        throw GameNotFoundException(slug, yearPublished)
    }

    data class LoadedGameView(private val inner: LoadedGame) {
        val slug: String get() = inner.slug
        val baseSchema: JsonNode get() = inner.baseSchema
        fun variantSchema(name: String): JsonNode? = inner.variantSchemas[name]
        val variantNames: Set<String> get() = inner.variantSchemas.keys
    }

    private fun buildSummary(game: LoadedGame): GameSummary {
        val playerProps = playerProperties(game.baseSchema)
        val identityEnum = playerProps
            ?.path("identity")
            ?.path("enum")
            ?.takeIf { it.isArray }
            ?.map { it.asText() }
            .orEmpty()
        val endStateNode = playerProps?.path("end_state")
        val propertyEnum = endStateNode?.path("propertyNames")?.path("enum")
            ?.takeIf { it.isArray }
            ?.map { it.asText() }
            .orEmpty()
        val propsNode = endStateNode?.path("properties")
        val fields = propertyEnum.map { key ->
            val spec = propsNode?.path(key)
            val type = when {
                spec == null || spec.isMissingNode -> "integer"
                spec.path("type").asText("") == "boolean" -> "boolean"
                else -> "integer"
            }
            EndStateFieldSpec(
                key = key,
                type = type,
                min = spec?.path("minimum")?.takeIf { it.isInt }?.asInt(),
                max = spec?.path("maximum")?.takeIf { it.isInt }?.asInt(),
            )
        }
        val supportsTeams = playerProps?.has("team") == true ||
            DISPLAY_NAMES[game.slug]?.supportsTeams == true
        val supportsElimination = playerProps?.has("eliminated") == true ||
            DISPLAY_NAMES[game.slug]?.supportsElimination == true
        val meta = DISPLAY_NAMES[game.slug] ?: GameMeta(game.slug.replaceFirstChar { it.titlecase() })
        return GameSummary(
            slug = game.slug,
            displayName = meta.displayName,
            yearPublished = game.yearPublished,
            identityOptions = identityEnum,
            endStateFields = fields,
            supportsTeams = meta.supportsTeams,
            supportsElimination = meta.supportsElimination,
            variants = game.variantSchemas.keys.sorted(),
        )
    }

    private fun playerProperties(schema: JsonNode): JsonNode? {
        val players = schema.path("properties").path("players")
        val items = players.path("items")
        val props = items.path("properties")
        return if (props.isMissingNode || props.isNull) null else props
    }

    private fun parseFolder(name: String): Pair<String, Int?> {
        val dot = name.lastIndexOf('.')
        if (dot <= 0) return name to null
        val tail = name.substring(dot + 1)
        val year = tail.toIntOrNull() ?: return name to null
        return name.substring(0, dot) to year
    }

    private data class GameMeta(
        val displayName: String,
        val supportsTeams: Boolean = false,
        val supportsElimination: Boolean = false,
    )

    companion object {
        private val DISPLAY_NAMES: Map<String, GameMeta> = mapOf(
            "bunny-kingdom" to GameMeta("Bunny Kingdom"),
            "calico" to GameMeta("Calico"),
            "catan" to GameMeta("Catan"),
            "codenames" to GameMeta("Codenames", supportsTeams = true),
            "coup" to GameMeta("Coup", supportsElimination = true),
            "everdell" to GameMeta("Everdell"),
            "hues-and-cues" to GameMeta("Hues and Cues"),
            "jaipur" to GameMeta("Jaipur"),
            "king-of-new-york" to GameMeta("King of New York", supportsElimination = true),
            "parks" to GameMeta("Parks"),
            "scythe" to GameMeta("Scythe"),
            "secret-hitler" to GameMeta("Secret Hitler", supportsTeams = true),
            "the-king-is-dead" to GameMeta("The King Is Dead"),
            "viticulture" to GameMeta("Viticulture"),
            "wavelength" to GameMeta("Wavelength", supportsTeams = true),
        )
    }
}
