# syntax=docker/dockerfile:1.7

# Build context is the parent directory (so board-game-record is included).
# See docker-compose.yml -> services.server.build.context: ..

FROM gradle:9.0.0-jdk25 AS build
WORKDIR /workspace

COPY board-game-server/settings.gradle.kts board-game-server/build.gradle.kts ./
COPY board-game-server/gradle gradle
COPY board-game-server/gradlew gradlew
RUN chmod +x gradlew

# Sibling checkout of board-game-record — needed at build time so
# `copyBoardGameSchemas` can stage JSON Schemas into resources.
COPY board-game-record ../board-game-record

COPY board-game-server/src src
RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
