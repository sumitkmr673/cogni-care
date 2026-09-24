#!/usr/bin/env bash
# Starts the voice service natively (it needs the GPU, so it does not run in Docker).
# Usage, from cogni-care/voice_service:   ./run.sh
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Parses KEY=VALUE lines (skips blanks/comments, strips quotes and a UTF-8 BOM) into the
# current shell's environment. $2.. optionally restricts which keys get imported.
import_env_file() {
    local file="$1"; shift
    local only_keys=("$@")
    [ -f "$file" ] || return 0
    local line key value k match
    while IFS= read -r line || [ -n "$line" ]; do
        line="${line%$'\r'}"
        line="${line#"${line%%[![:space:]]*}"}"
        line="${line%"${line##*[![:space:]]}"}"
        [ -z "$line" ] && continue
        case "$line" in \#*) continue ;; esac
        case "$line" in *=*) ;; *) continue ;; esac
        key="${line%%=*}"
        key="${key%"${key##*[![:space:]]}"}"
        value="${line#*=}"
        value="${value#"${value%%[![:space:]]*}"}"
        value="${value%"${value##*[![:space:]]}"}"
        value="${value#\"}"; value="${value%\"}"
        value="${value#\'}"; value="${value%\'}"
        if [ "${#only_keys[@]}" -eq 0 ]; then
            export "$key=$value"
        else
            match=0
            for k in "${only_keys[@]}"; do [ "$k" = "$key" ] && match=1 && break; done
            [ "$match" -eq 1 ] && export "$key=$value"
        fi
    done < <(sed '1s/^\xEF\xBB\xBF//' "$file")
}

# The JWT secret must match the main backend's, so take it from there; then local overrides.
import_env_file "../backend/.env" JWT_SECRET_KEY JWT_ALGORITHM
import_env_file ".env"

BIND_HOST="${VOICE_HOST:-127.0.0.1}"
PORT="${VOICE_PORT:-8100}"

exec ".venv/bin/python" -m uvicorn app.main:app --host "$BIND_HOST" --port "$PORT"
