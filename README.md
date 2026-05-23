# board-game-server

Kotlin + Spring Boot service that stores board-game session records and exposes a small REST API for the iOS app. Records are validated at ingest against the JSON Schemas in the sibling [`board-game-record`](../board-game-record/) repo (core + per-game base + any variants).

## Stack

- Java 25 toolchain (bytecode targets JVM 24 — Kotlin 2.2.0 doesn't expose a `JVM_25` constant yet; Java 25 runs JVM 24 bytecode without issue)
- Kotlin 2.2, Spring Boot 4.0.5, Gradle Kotlin DSL
- PostgreSQL 18.3 (JSONB columns for flexible player end-states)
- Flyway for schema migrations (via the per-subsystem `spring-boot-flyway` module)
- `com.networknt:json-schema-validator` for Draft 2020-12 validation (Jackson 2) — alongside Spring Boot 4's Jackson 3 on the HTTP layer
- Docker Compose for both local dev and production-style runs

## Layout

```
src/main/kotlin/com/sanchitb/boardgame/
├── BoardGameServerApplication.kt
├── api/             # controllers + DTOs
├── config/          # Jackson, API-key filter, filter registration
├── domain/          # JPA entity
├── repo/            # Spring Data repository
├── service/         # GameCatalogService, SchemaValidator, RecordService
└── error/           # ApiError, exception handler
```

Schemas are copied at build time from `../board-game-record/` via the `copyBoardGameSchemas` Gradle task. The server assumes that sibling repo is checked out alongside this one.

## Configuration

All settings are env-overridable. Defaults are for a dev postgres on localhost.

| Variable | Default | Notes |
|----------|---------|-------|
| `SERVER_PORT` | `8080` | HTTP port. |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/boardgame` | JDBC URL. |
| `DATABASE_USER` | `boardgame` | |
| `DATABASE_PASSWORD` | `boardgame` | |
| `BOARD_GAME_API_KEY` | `dev-key` | Clients must send this as `X-API-Key`. Rotate for prod. |
| `SPRING_PROFILES_ACTIVE` | `default` | Set to `prod` for prod-style logging. |
| `APPLE_BUNDLE_ID` | `com.sanchitb.boardgame.app` | `aud` claim the server requires on Apple ID tokens. |
| `BOARD_GAME_JWT_SECRET` | `dev-only-secret…` | HMAC signing key for session JWTs. **Set a real one in prod.** |
| `BOARD_GAME_JWT_TTL_HOURS` | `720` | Session token lifetime (default 30 days). |

## Authentication

Every `/api/*` route except `/api/auth/**` requires a session JWT in the `Authorization: Bearer <token>` header (401 otherwise). Clients get one by exchanging an Apple ID token:

```
POST /api/auth/apple
{ "identity_token": "<eyJraWQi…>", "full_name": "Alex Example" }
→ 200 { "token": "eyJ0eXAi…", "expires_at": "…", "user": { "id": "…", "email": "…", "display_name": "…" } }
```

The server verifies the Apple token's RS256 signature against `https://appleid.apple.com/auth/keys`, then upserts a user row keyed by the Apple `sub` and mints an HS256 session JWT. `full_name` is only sent by Apple on the first sign-in; pass it through so the server can store a display name.

## REST API

All `/api/*` routes (except `/api/auth/**`) require `Authorization: Bearer <token>`.

| Method | Path | Body / Params | Returns |
|--------|------|---------------|---------|
| POST | `/api/auth/apple` | `{identity_token, full_name?}` | 200 with `{token, expires_at, user}` |
| GET | `/api/games` | — | List of supported games with identity enums and end-state field specs |
| GET | `/api/games/{slug}` | — | Single game summary |
| POST | `/api/records` | Core record JSON (snake_case) | 201 with stored record, attributed to caller |
| GET | `/api/records` | `game=<slug>&limit=<n>` optional | Caller's records, newest first |
| GET | `/api/records/{id}` | — | Single record (only if owned by caller) |
| GET | `/api/players` | — | Caller's saved player roster |
| POST | `/api/players` | `{name, email?, notes?}` | 201 with stored player |
| PUT | `/api/players/{id}` | `{name, email?, notes?}` | Updated player |
| DELETE | `/api/players/{id}` | — | 204 |

All payloads use `snake_case` keys (e.g. `player_count`, `end_state`) to match the canonical record format in `board-game-record`.

On validation failure the server returns 400 with a `violations` array — each entry is a JSON Pointer (`/path/to/field`) plus a message, so clients can surface per-field errors.

## Local development (Docker Compose)

```bash
cp .env.example .env            # set BOARD_GAME_JWT_SECRET etc.
docker compose up -d postgres   # postgres:18.3 only
./gradlew bootRun               # runs the server on :8080 against dockerised postgres
```

Smoke test (auth rejection — actual Apple tokens come from the iOS app):

```bash
curl -s -o /dev/null -w "status=%{http_code}\n" http://localhost:8080/api/games
# → status=401

curl -s -X POST -H "Content-Type: application/json" \
  -d '{"identity_token":"not-a-jwt"}' \
  http://localhost:8080/api/auth/apple | jq
# → 401 with explanatory violation
```

## Production-style run (Docker Compose overlay)

Runs the server image and postgres together, no source mounts, logging tightened, restart policies on:

```bash
cp .env.example .env     # set a real API key and DB password
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
docker compose logs -f server
```

Note: the build context for the server image is the parent directory so the sibling `board-game-record` checkout can be copied in (see `Dockerfile` and the `build.context: ..` line in compose).

## Tests

```bash
./gradlew test
```

The schema-validation suite uses every `example.json` in `../board-game-record/games/**/` as a fixture — all of them must pass.

## Notes

- No variant schemas are checked into `board-game-record` yet; the server already supports them via the merge logic in `SchemaValidator`.
- `coup` uses the year-disambiguated folder `coup.2012`; records must set `year_published: 2012` so the catalog resolver finds it.
