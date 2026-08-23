#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$ROOT/docker-compose.yml"

if [ $# -eq 0 ]; then
  set -- --help
fi

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  exec docker compose -f "$COMPOSE_FILE" run --rm autotest-tool "$@"
fi

if command -v docker-compose >/dev/null 2>&1; then
  exec docker-compose -f "$COMPOSE_FILE" run --rm autotest-tool "$@"
fi

echo "docker compose (or docker-compose) not found" >&2
exit 127
