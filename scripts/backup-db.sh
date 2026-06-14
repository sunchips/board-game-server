#!/usr/bin/env bash
#
# Daily Postgres backup → Google Drive via rclone.
# Keeps the last 3 daily backups and prunes older ones.
#
# Prerequisites (one-time, on the server):
#   1. Install rclone:       curl https://rclone.org/install.sh | sudo bash
#   2. Configure Google Drive: rclone config  (create a remote named "gdrive")
#   3. Create the target folder in Drive, or let rclone create it.
#
# The systemd timer (board-game-backup.timer) runs this once per day.
# Manual usage:  ~/apps/board-game/board-game-server/scripts/backup-db.sh

set -euo pipefail

readonly DEPLOY_DIR="${DEPLOY_DIR:-$HOME/apps/board-game}"
readonly SERVER_DIR="$DEPLOY_DIR/board-game-server"
readonly BACKUP_DIR="/tmp/boardgame-backups"
readonly RCLONE_REMOTE="${RCLONE_REMOTE:-gdrive}"
readonly RCLONE_PATH="${RCLONE_PATH:-BoardGameBackups}"
readonly KEEP_DAYS=3

readonly CONTAINER="boardgame-postgres"
readonly DB_NAME="${POSTGRES_DB:-boardgame}"
readonly DB_USER="${POSTGRES_USER:-boardgame}"

export DOCKER_HOST="${DOCKER_HOST:-unix:///run/user/$(id -u)/docker.sock}"

log() { printf '[%s] %s\n' "$(date -u +%FT%TZ)" "$*"; }

mkdir -p "$BACKUP_DIR"

timestamp=$(date -u +%Y%m%d)
filename="boardgame-${timestamp}.sql.gz"
filepath="$BACKUP_DIR/$filename"

if [[ -f "$filepath" ]]; then
    log "backup already exists for today: $filename"
else
    log "dumping $DB_NAME from container $CONTAINER"
    docker exec "$CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$filepath"
    log "dump complete: $(du -h "$filepath" | cut -f1)"
fi

log "uploading $filename to ${RCLONE_REMOTE}:${RCLONE_PATH}/"
rclone copy "$filepath" "${RCLONE_REMOTE}:${RCLONE_PATH}/" --progress

log "pruning remote backups older than ${KEEP_DAYS} days"
rclone delete "${RCLONE_REMOTE}:${RCLONE_PATH}/" --min-age "${KEEP_DAYS}d" --include "boardgame-*.sql.gz"

log "pruning local backups older than ${KEEP_DAYS} days"
find "$BACKUP_DIR" -name "boardgame-*.sql.gz" -mtime +$KEEP_DAYS -delete

log "done"
