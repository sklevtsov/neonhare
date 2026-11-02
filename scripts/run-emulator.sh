#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
sdk_path="${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties)}"
adb_bin="$sdk_path/platform-tools/adb"
export ANDROID_HOME="$sdk_path"
export ANDROID_AVD_HOME="$PWD/.android-dev/avd"
if [[ ! -x "$sdk_path/emulator/emulator" ]]; then
  echo 'Android emulator is missing from the SDK in local.properties.' >&2
  exit 1
fi
# Always select an emulator explicitly; never install a development build on a handheld.
if ! "$adb_bin" devices | awk '$1=="emulator-5554" { found=1 } END { exit !found }'; then
  mkdir -p build
  nohup "$sdk_path/emulator/emulator" -avd NeonHare -port 5554 -no-snapshot -no-boot-anim -gpu host -no-audio > build/emulator.log 2>&1 &
fi
./gradlew assembleDebug
for ((attempt=0; attempt<180; attempt++)); do
  if [[ "$("$adb_bin" -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then
    "$adb_bin" -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
    "$adb_bin" -s emulator-5554 shell am start -W -n com.neonhare.game.debug/com.neonhare.game.MainActivity
    exit 0
  fi
  sleep 1
done
echo 'Emulator boot timed out; see build/emulator.log.' >&2
exit 1
