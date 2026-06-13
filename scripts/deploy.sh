#!/usr/bin/env bash
#
# Self-hosted CD entrypoint. Runs on the server (via the
# board-game-deploy.timer systemd user unit). Idempotent: a no-op if neither
# board-game-server nor board-game-record has new commits on origin/main.
#
# Manual usage (smoke test, force redeploy after editing this script):
#   ~/apps/board-game/board-game-server/scripts/deploy.sh
#
# Layout assumed on the server:
#   ~/apps/board-game/
#     board-game-server/    (this repo)
#     board-game-record/    (sibling, used by docker build context)
#
# Override targets for testing:
#   DEPLOY_DIR=/tmp/staging ./scripts/deploy.sh

set -euo pipefail

readonly DEPLOY_DIR="${DEPLOY_DIR:-$HOME/apps/board-game}"
readonly SERVER_DIR="$DEPLOY_DIR/board-game-server"
readonly RECORD_DIR="$DEPLOY_DIR/board-game-record"
readonly VERSION_URL="${VERSION_URL:-http://127.0.0.1:28080/version}"
readonly LOCK="/tmp/board-game-deploy.lock"

# Rootless Docker context. Without this, non-interactive shells (systemd,
# cron) default to the root daemon socket which doesn't know about our stack.
export DOCKER_HOST="${DOCKER_HOST:-unix:///run/user/$(id -u)/docker.sock}"

log() { printf '[%s] %s\n' "$(date -u +%FT%TZ)" "$*"; }

# Single-flight: serialise concurrent invocations from the webhook + timer.
# Block (don't drop) so a webhook firing during a slow build queues behind
# it. The queued run will git-fetch fresh state when its turn comes — if
# nothing new arrived, it short-circuits on the SHA comparison below.
exec 9>"$LOCK"
flock 9

for d in "$SERVER_DIR" "$RECORD_DIR"; do
    if [[ ! -d "$d/.git" ]]; then
        log "missing git checkout: $d"
        exit 1
    fi
done

git -C "$SERVER_DIR" fetch --quiet origin
git -C "$RECORD_DIR" fetch --quiet origin

server_local=$(git -C "$SERVER_DIR" rev-parse HEAD)
server_remote=$(git -C "$SERVER_DIR" rev-parse origin/main)
record_local=$(git -C "$RECORD_DIR" rev-parse HEAD)
record_remote=$(git -C "$RECORD_DIR" rev-parse origin/main)

if [[ "$server_local" == "$server_remote" && "$record_local" == "$record_remote" ]]; then
    # Quiet on the no-op path so journalctl stays readable.
    exit 0
fi

log "deploying: server ${server_local:0:7} -> ${server_remote:0:7}, record ${record_local:0:7} -> ${record_remote:0:7}"

git -C "$RECORD_DIR" pull --ff-only --quiet
git -C "$SERVER_DIR" pull --ff-only --quiet

expected_sha=$(git -C "$SERVER_DIR" rev-parse HEAD)

cd "$SERVER_DIR"
GIT_COMMIT="$expected_sha" docker compose -f docker-compose.yml -f docker-compose.prod.yml build
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Spring Boot takes ~15-20s to start. Backoff sequence sums to ~140s — more
# than enough headroom for a cold JIT.
for delay in 2 3 5 8 13 21 34 55; do
    deployed_sha=$(curl -s --max-time 5 "$VERSION_URL" | grep -o '"commit":"[^"]*"' | cut -d'"' -f4 || true)
    if [[ "$deployed_sha" == "$expected_sha" ]]; then
        log "verified: ${expected_sha:0:7} is live"
        exit 0
    fi
    sleep "$delay"
done

log "WARNING: expected SHA ${expected_sha:0:7} but got ${deployed_sha:-none}. Container may still recover; check docker logs boardgame-server."
exit 1
