#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_DIR="$ROOT/android"

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle was not found. Install Android Studio or Gradle 8.9+ and retry." >&2
  exit 1
fi
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo "Set ANDROID_HOME or ANDROID_SDK_ROOT to your Android SDK directory." >&2
  exit 1
fi

gradle -p "$ANDROID_DIR" :app:assembleDebug

echo "APK: $ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
