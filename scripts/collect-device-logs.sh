#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
sdk_path="${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties)}"
adb_bin="$sdk_path/platform-tools/adb"
device="${1:?Usage: collect-device-logs.sh DEVICE_SERIAL}"
destination="build/diagnostics/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$destination"
"$adb_bin" -s "$device" logcat -d -b crash -v threadtime > "$destination/crash-buffer.txt"
"$adb_bin" -s "$device" logcat -d -b main -b system -b events -v threadtime > "$destination/logcat.txt"
"$adb_bin" -s "$device" shell dumpsys activity exit-info com.neonhare.game > "$destination/exit-info.txt"
"$adb_bin" -s "$device" pull /sdcard/Android/data/com.neonhare.game/files/diagnostics "$destination/game-events" || printf 'No persistent game journal on this device yet.\n'
printf 'Saved logs (buffers preserved): %s\n' "$destination"
