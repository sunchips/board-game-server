package com.sanchitb.boardgame.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import com.sanchitb.boardgame.error.SchemaValidationException
import com.sanchitb.boardgame.error.Violation
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

@Service
class SchemaValidator(
    @Qualifier("legacyObjectMapper")
    private val objectMapper: ObjectMapper,
    private val catalog: GameCatalogService,
) {
    private lateinit var coreSchema: JsonSchema
    private lateinit var factory: JsonSchemaFactory
    private lateinit var config: SchemaValidatorsConfig

    @PostConstruct
    fun init() {
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        config = SchemaValidatorsConfig.builder().build()
        val coreBytes = ClassPathResource("schemas/core.schema.json")
            .inputStream.readBytes().toString(StandardCharsets.UTF_8)
        coreSchema = factory.getSchema(coreBytes, config)
    }

    fun validate(recordJson: JsonNode) {
        val violations = mutableListOf<Violation>()
        violations.addAll(coreSchema.validate(recordJson).toViolations())

        val game = recordJson.path("game").asText(null) ?: return failIfAny(violations, "Core validation failed")
        val yearPublished = recordJson.path("year_published").let { if (it.isInt) it.asInt() else null }
        val variants = recordJson.path("variants").toStringList()

        val loaded = runCatching { catalog.resolveGame(game, yearPublished) }.getOrElse {
            violations.add(Violation("/game", it.message ?: "game not found"))
            failIfAny(violations, "Game not found")
            return
        }

        val schemas = mutableListOf<Pair<String, JsonNode>>(loaded.slug to loaded.baseSchema)
        for (v in variants) {
            val vs = loaded.variantSchema(v)
            if (vs == null) {
                violations.add(Violation("/variants", "unknown variant '$v' for game '${loaded.slug}'"))
            } else {
                schemas.add(v to vs)
            }
        }

        val (merged, conflicts) = mergeVariantSchemas(schemas)
        for (c in conflicts) violations.add(Violation("/variants", c))

        val mergedSchema = factory.getSchema(merged, config)
        violations.addAll(mergedSchema.validate(recordJson).toViolations())

        // Cross-field rules
        val playersNode = recordJson.path("players")
        if (playersNode.isArray) {
            val playerCountNode = recordJson.path("player_count")
            if (playerCountNode.isInt && playerCountNode.asInt() != playersNode.size()) {
                violations.add(
                    Violation(
                        "/player_count",
                        "${playerCountNode.asInt()} does not match len(players)=${playersNode.size()}",
                    ),
                )
            }
            val winnersNode = recordJson.path("winners")
            if (winnersNode.isArray) {
                winnersNode.forEachIndexed { i, w ->
                    if (w.isInt && w.asInt() >= playersNode.size()) {
                        violations.add(
                            Violation(
                                "/winners/$i",
                                "index ${w.asInt()} out of range (players has ${playersNode.size()} entries)",
                            ),
                        )
                    }
                }
            }
        }

        failIfAny(violations, "Record failed schema validation")
    }

    private fun failIfAny(violations: List<Violation>, message: String) {
        if (violations.isNotEmpty()) throw SchemaValidationException(message, violations)
    }

    private fun Set<ValidationMessage>.toViolations(): List<Violation> = map { msg ->
        Violation(path = msg.instanceLocation.toString().ifEmpty { "/" }, message = msg.message)
    }

    private fun JsonNode.toStringList(): List<String> =
        if (this.isArray) this.mapNotNull { if (it.isTextual) it.asText() else null } else emptyList()

    private fun mergeVariantSchemas(schemas: List<Pair<String, JsonNode>>): Pair<JsonNode, List<String>> {
        val endStateKeys = sortedSetOf<String>()
        val endStateProps = mutableMapOf<String, JsonNode>()
        val endStateOwner = mutableMapOf<String, String>()
        val identityValues = sortedSetOf<String>()
        var identityConstrained = false
        val conflicts = mutableListOf<String>()

        for ((label, schema) in schemas) {
            val pprops = schema.path("properties").path("players").path("items").path("properties")
            val endState = pprops.path("end_state")
            endState.path("propertyNames").path("enum").forEach { endStateKeys.add(it.asText()) }
            val props = endState.path("properties")
            if (props.isObject) {
                props.fieldNames().forEach { key ->
                    val spec = props.get(key)
                    val existing = endStateProps[key]
                    if (existing != null && existing != spec) {
                        conflicts.add(
                            "end_state.$key: incompatible definitions in '${endStateOwner[key]}' and '$label'",
                        )
                    } else {
                        endStateProps[key] = spec
                        endStateOwner[key] = label
                    }
                }
            }
            val identity = pprops.path("identity")
            if (identity.has("enum") && identity.path("enum").isArray) {
                identityConstrained = true
                identity.path("enum").forEach { identityValues.add(it.asText()) }
            }
        }

        val playerProps = objectMapper.createObjectNode()
        val endStateSchema = objectMapper.createObjectNode().apply {
            put("type", "object")
            val propertyNames = objectMapper.createObjectNode()
            val keyEnum = objectMapper.createArrayNode()
            endStateKeys.forEach(keyEnum::add)
            propertyNames.set<ObjectNode>("enum", keyEnum)
            set<ObjectNode>("propertyNames", propertyNames)
            val propsNode = objectMapper.createObjectNode()
            for ((k, v) in endStateProps) propsNode.set<ObjectNode>(k, v)
            set<ObjectNode>("properties", propsNode)
            val addl = objectMapper.createObjectNode()
            val addlTypes = objectMapper.createArrayNode()
            addlTypes.add("integer")
            addlTypes.add("boolean")
            addl.set<ObjectNode>("type", addlTypes)
            set<ObjectNode>("additionalProperties", addl)
        }
        playerProps.set<ObjectNode>("end_state", endStateSchema)
        if (identityConstrained) {
            val identitySchema = objectMapper.createObjectNode()
            val identityEnum: ArrayNode = objectMapper.createArrayNode()
            identityValues.forEach(identityEnum::add)
            identitySchema.set<ObjectNode>("enum", identityEnum)
            playerProps.set<ObjectNode>("identity", identitySchema)
        }

        val merged = objectMapper.createObjectNode().apply {
            put("\$schema", "https://json-schema.org/draft/2020-12/schema")
            put("type", "object")
            val properties = objectMapper.createObjectNode()
            val playersSchema = objectMapper.createObjectNode().apply {
                put("type", "array")
                val items = objectMapper.createObjectNode().apply {
                    put("type", "object")
                    set<ObjectNode>("properties", playerProps)
                }
                set<ObjectNode>("items", items)
            }
            properties.set<ObjectNode>("players", playersSchema)
            set<ObjectNode>("properties", properties)
        }
        return merged to conflicts
    }
}
