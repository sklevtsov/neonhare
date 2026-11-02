#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
sdk_path="${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties)}"
adb_bin="$sdk_path/platform-tools/adb"
python3 tools/level_content.py
./gradlew checkJavaFormat assembleDebug assembleDebugAndroidTest
"$adb_bin" -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
"$adb_bin" -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
check_output="$("$adb_bin" -s emulator-5554 shell am instrument -w com.neonhare.game.debug.test/com.neonhare.game.GameChecks)"
printf '%s\n' "$check_output"
[[ "$check_output" == *'PASS:'* && "$check_output" != *'FAIL:'* ]]
