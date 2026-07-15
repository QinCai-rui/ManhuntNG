#!/usr/bin/env bash
# Build ManhuntNG Pumpkin plugin
# Requires: pip install pumpkin-api-py

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Installing dependencies..."
pip install -r requirements.txt 2>/dev/null || true

echo "Building ManhuntNG plugin..."
pumpkin-plugin-build main -o manhuntng.wasm

echo "Build complete! Output: manhuntng.wasm"
echo "Place this file in your Pumpkin server's plugins/ directory."
