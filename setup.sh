#!/usr/bin/env bash
# Link the SuperCollider extension, install dependencies, and build the MCP server.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

case "$(uname)" in
  Darwin) EXTENSIONS="$HOME/Library/Application Support/SuperCollider/Extensions" ;;
  *)      EXTENSIONS="${XDG_DATA_HOME:-$HOME/.local/share}/SuperCollider/Extensions" ;;
esac

echo "Linking ClaudeCollider into $EXTENSIONS"
mkdir -p "$EXTENSIONS"
ln -sfn "$ROOT/ClaudeCollider" "$EXTENSIONS/ClaudeCollider"

echo "Installing dependencies"
(cd "$ROOT" && npm install)

echo "Building"
(cd "$ROOT" && npm run build)
