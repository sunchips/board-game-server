import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.sanchitb"
version = "0.1.0"

// Build uses the Java 25 toolchain on developer machines (the user's installed
// JDK); bytecode targets JVM 24 because Kotlin 2.2.0's jvmTarget tops out at
// 24 and the Docker builder uses `gradle:9.0.0-jdk24` (no jdk25 image yet).
// A Java 25+ runtime runs this bytecode cleanly.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget("24"))
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Spring Boot 4 serialises HTTP bodies with Jackson 3 (tools.jackson.*). Pull in
    // its Kotlin module so our Kotlin data classes round-trip on the HTTP layer.
    // java.time support is built into Jackson 3 core — no separate jsr310 datatype.
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.0")
    // Jackson 2 is still required because networknt/json-schema-validator speaks
    // com.fasterxml.jackson.databind.JsonNode — we parse request bodies and build
    // merged schemas as Jackson 2 trees purely for validation.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // Spring Boot 4 split auto-config into per-subsystem modules; spring-boot-flyway
    // provides FlywayAutoConfiguration (equivalent to what was in spring-boot-autoconfigure in 3.x).
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.networknt:json-schema-validator:1.5.2")
    // Apple Sign-In: verify Apple's RS256 ID tokens, issue our own HS256 session JWTs.
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.auth0:jwks-rsa:0.22.1")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val schemaSource = layout.projectDirectory.dir("../board-game-record")
val schemaDest = layout.buildDirectory.dir("generated-resources")

val copyBoardGameSchemas by tasks.registering(Copy::class) {
    description = "Copies JSON Schemas from the sibling board-game-record checkout into server resources under /schemas."
    val coreSchema = schemaSource.file("schema/core.schema.json")
    val gamesDir = schemaSource.dir("games")
    onlyIf {
        val ok = coreSchema.asFile.exists() && gamesDir.asFile.exists()
        if (!ok) {
            logger.warn(
                "board-game-record not found at {} — schemas will be missing. " +
                    "Clone the sibling repo or set -PschemaSource=<path>.",
                schemaSource.asFile.absolutePath,
            )
        }
        ok
    }
    into(schemaDest.map { it.dir("schemas") })
    from(coreSchema) {
        into("")
    }
    from(gamesDir) {
        into("games")
        include("**/*.schema.json")
    }
}

sourceSets {
    main {
        resources {
            srcDir(schemaDest)
        }
    }
}

tasks.named("processResources") {
    dependsOn(copyBoardGameSchemas)
}
