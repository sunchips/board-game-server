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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
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
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.networknt:json-schema-validator:1.5.2")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val schemaSource = layout.projectDirectory.dir("../board-game-record")
val schemaDest = layout.buildDirectory.dir("generated-resources/schemas")

val copyBoardGameSchemas by tasks.registering(Copy::class) {
    description = "Copies JSON Schemas from the sibling board-game-record checkout into server resources."
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
    into(schemaDest)
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
