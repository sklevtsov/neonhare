#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/security-checks
if ./scripts/package-apk.sh > build/security-checks/no-mode.txt 2>&1; then
  echo 'FAIL: signing accepted an implicit mode'; exit 1
fi
if env -u NEONHARE_RELEASE_KEYSTORE -u NEONHARE_RELEASE_ALIAS -u NEONHARE_STORE_PASSWORD -u NEONHARE_KEY_PASSWORD -u NEONHARE_RELEASE_CERT_SHA256 ./scripts/package-apk.sh --release > build/security-checks/no-key.txt 2>&1; then
  echo 'FAIL: public release accepted missing signing secrets'; exit 1
fi
if NEONHARE_RELEASE_KEYSTORE=placeholder-release.jks NEONHARE_RELEASE_ALIAS=androiddebugkey NEONHARE_STORE_PASSWORD=contract-test NEONHARE_KEY_PASSWORD=contract-test NEONHARE_RELEASE_CERT_SHA256=00 ./scripts/package-apk.sh --release > build/security-checks/debug-key.txt 2>&1; then
  echo 'FAIL: public release accepted the debug alias'; exit 1
fi
rg -q 'refuses a debug' build/security-checks/debug-key.txt
printf 'PASS: explicit signing mode, missing release secrets and debug alias rejection\n'
