#!/usr/bin/env bash
set -euo pipefail

# Preview must start the packaged UI, exit cleanly, and leave no runtime data.
workspace=$(mktemp -d)
pid=''
cleanup() {
  if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
  fi
  rm -rf "$workspace"
}
trap 'cleanup' EXIT
mkdir "$workspace/data"
TEAM293_TIME_DATA_DIR="$workspace/data" "$@" --preview --smoke-test >"$workspace/output" 2>&1 &
pid=$!

for ((attempt = 0; attempt < 30; attempt++)); do
  if ! kill -0 "$pid" 2>/dev/null; then
    if ! wait "$pid"; then
      printf 'Packaged preview exited unsuccessfully.\n' >&2
      exit 1
    fi
    pid=''
    if ! grep -qx 'PREVIEW_READY' "$workspace/output"; then
      printf 'Packaged preview did not report UI readiness.\n' >&2
      exit 1
    fi
    if [[ -n "$(find "$workspace/data" -mindepth 1 -print -quit)" ]]; then
      printf 'Preview unexpectedly created runtime data.\n' >&2
      exit 1
    fi
    cleanup
    trap - EXIT
    printf 'Packaged preview passed; no runtime data created.\n'
    exit 0
  fi
  sleep 1
done
printf 'Packaged preview timed out.\n' >&2
exit 1
