#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
sdk_path="${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties)}"
adb_bin="$sdk_path/platform-tools/adb"
./gradlew -PtestRunner=com.neonhare.game.ScreenshotCapture assembleDebug assembleDebugAndroidTest
"$adb_bin" -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
"$adb_bin" -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
output="$("$adb_bin" -s emulator-5554 shell am instrument -w com.neonhare.game.debug.test/com.neonhare.game.ScreenshotCapture)"
printf '%s\n' "$output"
[[ "$output" == *'PASS:'* && "$output" != *'FAIL:'* ]]
mkdir -p docs/media
names=(green-rift lev-arden floating-gardens crystal-memory)
for index in 0 1 2 3; do
  "$adb_bin" -s emulator-5554 exec-out run-as com.neonhare.game.debug cat "files/screenshot-$index.png" > "docs/media/${names[$index]}.png"
done
