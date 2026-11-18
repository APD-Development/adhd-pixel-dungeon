#!/bin/bash
# Runs the instrumented tests with logcat captured to files that are uploaded even when the run hangs.
# The gradle step is bounded (14 min, the normal run is about 5) so a hang FAILS the job and leaves its evidence, instead of
# silently using up the job's 45 minutes with no logs.
adb logcat -c
adb logcat -v time > logcat.txt &
LOGCAT_PID=$!
# the migration test's own step markers (and its watchdog's thread dump) and the timing numbers go straight into this step's log, live
adb logcat -v time APDMIG:V APDPERF:V APDUI:V '*:S' &
MARKS_PID=$!
timeout --signal=TERM --kill-after=30 840 ./gradlew :android:connectedDebugAndroidTest
STATUS=$?
if [ "$STATUS" = "124" ]; then echo "DEVICE TESTS DID NOT FINISH IN 14 MINUTES (see logcat artifact)"; fi
sleep 2
adb logcat -d -v time > logcat-final.txt || true
echo "=== last lines of the APDMIG markers ==="
grep -a "APDMIG\|APDPERF\|APDUI" logcat-final.txt | tail -60
echo "=== crashes / ANR / test-process errors ==="
grep -a -E "FATAL EXCEPTION|ANR in|AndroidRuntime|am_proc_died|Process .* has died|INSTRUMENTATION_" logcat-final.txt | tail -40
kill "$LOGCAT_PID" "$MARKS_PID" 2>/dev/null || true
exit "$STATUS"
