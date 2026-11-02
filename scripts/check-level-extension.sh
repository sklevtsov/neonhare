#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
chapter_id=refactor-content-contract
folder="app/src/main/assets/levels/$chapter_id"
index=app/src/main/assets/levels/campaign.json
[[ ! -e "$folder" ]] || { echo 'Extension contract folder already exists; refusing overwrite.' >&2; exit 2; }
mkdir -p build/refactor-baseline
original="$(mktemp build/refactor-baseline/campaign.XXXXXX)"
cp "$index" "$original"
restore() {
  cp "$original" "$index"
  rm -f "$original"
  rm -rf "$folder"
}
trap restore EXIT
python3 scripts/new-level.py "$chapter_id" --title 'Проверка отдельной новой главы' --append
./scripts/check-game.sh
