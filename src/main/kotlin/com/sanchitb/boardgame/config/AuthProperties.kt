package com.sanchitb.boardgame.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auth")
data class AuthProperties(
    val jwtSecret: String,
    val jwtTtlSeconds: Long,
    val jwtIssuer: String,
    val apple: Apple,
) {
    data class Apple(
        val clientId: String,
        val issuer: String,
        val jwksUrl: String,
    )
}
