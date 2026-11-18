#!/bin/bash
# Installs the release APK (smoke.apk), launches it, and fails if it crashes or is not running after 60 seconds.
PKG=com.charltonchris29.adhdpixeldungeon
adb install -r smoke.apk || exit 1
adb shell dumpsys package "$PKG" | grep -E "versionCode|versionName|targetSdk" | head -5
adb logcat -c
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 20
adb exec-out screencap -p > smoke-screen-20s.png
sleep 40
adb exec-out screencap -p > smoke-screen-60s.png
adb logcat -d -v time > smoke-logcat.txt
PID=$(adb shell pidof "$PKG" | tr -d '\r')
echo "=== crashes for $PKG ==="
CRASH=$(grep -a -E "FATAL EXCEPTION|ANR in $PKG|Process: $PKG" smoke-logcat.txt | head -20)
echo "${CRASH:-none}"
if [ -z "$PID" ]; then echo "FAIL: $PKG is not running after 60 seconds"; exit 1; fi
if [ -n "$CRASH" ]; then echo "FAIL: crash in logcat"; exit 1; fi
for f in smoke-screen-20s.png smoke-screen-60s.png; do
  C=$(python3 .github/ci/png-colors.py "$f" 2>/dev/null || echo 0)
  echo "$f: $(stat -c %s "$f") bytes, $C distinct colors"
  if [ "${C:-0}" -lt 50 ]; then echo "FAIL: $f looks blank ($C colors)"; exit 1; fi
done
echo "PASS: $PKG running as pid $PID after 60 seconds with no crash"
