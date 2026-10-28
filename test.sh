#!/usr/bin/env bash
# Run the SuperCollider test suite, using the same sclang the MCP server would
# (see SclangConfig in src/config.ts).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

if [[ ! -f "$ROOT/dist/config.js" ]]; then
  echo "error: dist/ not built. Run ./setup.sh first." >&2
  exit 1
fi

SCLANG="$(cd "$ROOT" && node --input-type=module -e \
  'import { SclangConfig } from "./dist/config.js"; console.log(new SclangConfig().path)')"

if ! command -v "$SCLANG" >/dev/null 2>&1; then
  echo "error: sclang not found at '$SCLANG'. Install SuperCollider or set SCLANG_PATH." >&2
  exit 1
fi

exec "$SCLANG" "$ROOT/ClaudeCollider/tests/run_tests.scd" "$@"
