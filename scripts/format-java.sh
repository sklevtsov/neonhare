#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ "${1:-}" == "--check" ]]; then exec ./gradlew checkJavaFormat; fi
exec ./gradlew formatJava
