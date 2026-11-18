#!/usr/bin/env bash
set -euo pipefail
adb logcat -c
adb logcat -v time > exact329-logcat.txt &
adb install -r exact329.apk
adb install -r android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk
# Preserve the same earlier Home font/layout case as the full suite before measuring.
adb shell am instrument -w -r -e class 'com.shatteredpixel.shatteredpixeldungeon.android.AndroidBackDeviceTest#homeTextKeepsItsWordsAndFitsOversizedTokens,com.shatteredpixel.shatteredpixeldungeon.android.AndroidBackDeviceTest#warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms,com.shatteredpixel.shatteredpixeldungeon.android.DiagnosticReportDeviceTest#actualReportButtonSavesDeviceEvidenceAndMenusSurvivePauseResume' com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner | tee title-instrumentation.txt
mkdir -p title-evidence
for name in diagnostic-report-button.png diagnostic-report-saved.png; do
    adb exec-out cat "/sdcard/Pictures/APD-Test-Visuals/$name" > "title-evidence/$name"
done
APD_EXPECTED_METHODS=3 APD_EXPECTED_IMAGES='diagnostic-report-button.png diagnostic-report-saved.png' python3 tools/record-native-confirmation-results.py
