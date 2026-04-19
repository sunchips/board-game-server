plugins {
    // Lets Gradle auto-provision JDK toolchains that aren't already installed
    // locally — required in fresh environments like the Docker builder.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "board-game-server"
