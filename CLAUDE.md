# Working agreements (server)

- **Run `./gradlew test` before every commit.** Don't trust a clean `compileKotlin` — the catalog tests and example-record validation only fire under `test`, and they're the ones that catch a new game wired up incorrectly. If tests fail, fix them first; never commit a red tree.
- **The server must boot.** After changes that touch config, migrations, or DI wiring, smoke-test with `./gradlew bootRun` (Postgres needs to be up — see README) before declaring the change done. The Spring context loading in `AuthenticatedApiTest` is a good proxy but not a substitute when the change is in startup wiring.
- **Same merge/commit rules as the iOS repo:** merge feature branches into main without asking, commit every logical change, don't push without an explicit ask.

## Cross-repo gotchas

- Adding a game means edits in three places: the schema files in `../board-game-record/games/<slug>/`, the `DISPLAY_NAMES` map in `GameCatalogService.kt`, and the `expected` slug set in `SchemaValidatorTest.kt`. Forget the last one and the test fails.
- `example.json` files have to satisfy `schema/core.schema.json` as well as the game's own schema. The most common trap: `winners` is an array of **integer indices** into `players[]`, not names.
- The Apple ID-token `aud` claim must match `apple.bundle-id` in `application.yml`. The iOS bundle id is `com.sanchitb.boardgamerecorder` — keep these aligned across the rename of either side.
