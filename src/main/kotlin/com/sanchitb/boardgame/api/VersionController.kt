package com.sanchitb.boardgame.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Properties

@RestController
class VersionController(
    @Value("classpath:version.properties") private val versionResource: Resource,
) {
    private val props: Properties by lazy {
        Properties().also { it.load(versionResource.inputStream) }
    }

    @GetMapping("/version")
    fun version(): Map<String, String> = mapOf(
        "commit" to (props.getProperty("git.commit") ?: "unknown"),
        "build_time" to (props.getProperty("build.time") ?: "unknown"),
    )
}
