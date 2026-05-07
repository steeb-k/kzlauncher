#!/usr/bin/env bash
#
# Build, install, and launch kzLauncher on a running emulator. If no
# emulator is running, start one first.
#
# Optional env overrides:
#   KZ_AVD        — name of the AVD to boot (default: first one returned
#                   by `emulator -list-avds`).
#   EMULATOR_BIN  — full path to the emulator binary, if it isn't on PATH
#                   and none of the standard SDK locations apply.

set -euo pipefail

PACKAGE="app.kzlauncher.debug"
ACTIVITY="$PACKAGE/app.olauncher.MainActivity"

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT_DIR"

# --- Resolve tools -----------------------------------------------------------

if ! command -v adb >/dev/null; then
    echo "ERROR: 'adb' is not on PATH." >&2
    exit 1
fi

emulator_bin=""
for candidate in \
    "${EMULATOR_BIN:-}" \
    "$(command -v emulator 2>/dev/null || true)" \
    "${ANDROID_HOME:-}/emulator/emulator" \
    "${ANDROID_SDK_ROOT:-}/emulator/emulator" \
    "$HOME/Android/Sdk/emulator/emulator" \
    "$HOME/Library/Android/sdk/emulator/emulator"
do
    if [[ -n "$candidate" && -x "$candidate" ]]; then
        emulator_bin="$candidate"
        break
    fi
done

# --- Look for an emulator that's already up ---------------------------------

# Match lines like:  emulator-5554   device
running_serial=$(adb devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ {print $1; exit}')

if [[ -z "$running_serial" ]]; then
    if [[ -z "$emulator_bin" ]]; then
        echo "ERROR: no running emulator, and the 'emulator' binary was not found." >&2
        echo "Set EMULATOR_BIN or ANDROID_HOME, or put 'emulator' on PATH." >&2
        exit 1
    fi

    avd="${KZ_AVD:-}"
    if [[ -z "$avd" ]]; then
        avd=$("$emulator_bin" -list-avds 2>/dev/null | head -n1 || true)
    fi
    if [[ -z "$avd" ]]; then
        echo "ERROR: no AVDs available. Create one in Android Studio or set KZ_AVD." >&2
        exit 1
    fi

    echo "==> Starting emulator: $avd"
    nohup "$emulator_bin" -avd "$avd" >/dev/null 2>&1 &
    disown

    echo "==> Waiting for device to come online"
    adb wait-for-device

    echo "==> Waiting for boot to complete"
    until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
        sleep 2
    done
    # Give the launcher a moment to settle before sideloading.
    sleep 2

    running_serial=$(adb devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ {print $1; exit}')
else
    echo "==> Reusing running emulator: $running_serial"
fi

# Pin every adb / gradle command below to this specific device.
export ANDROID_SERIAL="$running_serial"

# --- Build, install, launch --------------------------------------------------

echo "==> Building and installing debug APK"
"$ROOT_DIR/gradlew" --console=plain :app:installDebug

echo "==> Restarting $PACKAGE"
adb shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
adb shell am start -n "$ACTIVITY"
