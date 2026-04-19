package com.sanchitb.boardgame.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    /**
     * Jackson 2 ObjectMapper used internally for JSON Schema validation (networknt
     * speaks Jackson 2) and for parsing request bodies before handing them to the
     * validator. Intentionally NOT @Primary — Spring Boot 4 installs a Jackson 3
     * ObjectMapper for HTTP message conversion and we leave that one in place.
     *
     * Uses snake_case so Kotlin camelCase fields map to the canonical record shape
     * (e.g. playerCount <-> player_count, endState <-> end_state).
     */
    @Bean("legacyObjectMapper")
    fun legacyObjectMapper(): ObjectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }
}
