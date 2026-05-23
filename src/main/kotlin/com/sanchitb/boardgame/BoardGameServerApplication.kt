package com.sanchitb.boardgame

import com.sanchitb.boardgame.config.AuthProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AuthProperties::class)
class BoardGameServerApplication

fun main(args: Array<String>) {
    runApplication<BoardGameServerApplication>(*args)
}
