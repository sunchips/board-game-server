package com.sanchitb.boardgame

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BoardGameServerApplication

fun main(args: Array<String>) {
    runApplication<BoardGameServerApplication>(*args)
}
