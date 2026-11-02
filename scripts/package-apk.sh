#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
mode="${1:-}"
sdk_path="${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties)}"
signer="$sdk_path/build-tools/36.0.0/apksigner"
version="$(sed -n "s/.*versionName '\([^']*\)'.*/\1/p" app/build.gradle | head -1)"
case "$mode" in
  --local)
    ./gradlew assembleLocal
    source_apk=app/build/outputs/apk/local/app-local-unsigned.apk
    destination="app/build/outputs/apk/local/neon-hare-$version-local.apk"
    "$signer" sign --ks "${ANDROID_KEYSTORE:-$HOME/.android/debug.keystore}" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$destination" "$source_apk"
    ;;
  --release)
    : "${NEONHARE_RELEASE_KEYSTORE:?Set the dedicated release keystore path}"
    : "${NEONHARE_RELEASE_ALIAS:?Set the release alias}"
    : "${NEONHARE_STORE_PASSWORD:?Set the keystore password in the environment}"
    : "${NEONHARE_KEY_PASSWORD:?Set the key password in the environment}"
    : "${NEONHARE_RELEASE_CERT_SHA256:?Pin the expected release certificate fingerprint}"
    if [[ "$NEONHARE_RELEASE_ALIAS" == androiddebugkey || "$NEONHARE_RELEASE_KEYSTORE" == *debug.keystore ]]; then
      echo 'Release signing refuses a debug keystore/alias.' >&2; exit 2
    fi
    ./gradlew assembleRelease
    mkdir -p build/signing
    signing_dir="$(mktemp -d build/signing/check.XXXXXX)"
    trap 'rm -rf "$signing_dir"' EXIT
    signed_candidate="$signing_dir/candidate.apk"
    "$signer" sign --ks "$NEONHARE_RELEASE_KEYSTORE" --ks-key-alias "$NEONHARE_RELEASE_ALIAS" --ks-pass env:NEONHARE_STORE_PASSWORD --key-pass env:NEONHARE_KEY_PASSWORD --out "$signed_candidate" app/build/outputs/apk/release/app-release-unsigned.apk
    "$signer" verify --print-certs "$signed_candidate" > "$signing_dir/certificate.txt"
    expected="$(printf '%s' "$NEONHARE_RELEASE_CERT_SHA256" | tr '[:upper:]' '[:lower:]' | tr -d ':')"
    actual="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$signing_dir/certificate.txt")"
    if [[ "$expected" != "$actual" ]] || grep -Fq 'CN=Android Debug' "$signing_dir/certificate.txt"; then
      echo 'Release certificate rejected; no distributable APK published.' >&2; exit 2
    fi
    destination="app/build/outputs/apk/release/neon-hare-$version.apk"
    mv "$signed_candidate" "$destination"
    ;;
  *) echo 'Usage: package-apk.sh --local | --release' >&2; exit 2 ;;
esac
"$signer" verify --verbose "$destination"
printf '\nAPK: %s\n' "$PWD/$destination"
