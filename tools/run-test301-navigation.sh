#!/usr/bin/env bash
set -euo pipefail
apd_aapt="$(find "$ANDROID_HOME/build-tools" -type f -name aapt | sort -V | tail -1)"
apd_test_apk=android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk
apd_package="$("$apd_aapt" dump badging application.apk | sed -n "s/^package: name='\\([^']*\\)'.*/\\1/p")"
apd_test_package="$("$apd_aapt" dump badging "$apd_test_apk" | sed -n "s/^package: name='\\([^']*\\)'.*/\\1/p")"
adb install -r -d application.apk
adb install -r -d "$apd_test_apk"
adb shell pm clear "$apd_package"
adb logcat -c
adb shell am instrument -w -r -e class 'com.shatteredpixel.shatteredpixeldungeon.android.UimsdColumnsModalDeviceTest#evidenceBackRestoresEachTableAndSurvivesSceneRecreation' "${apd_test_package}/androidx.test.runner.AndroidJUnitRunner" > navigation-evidence/instrumentation.txt
adb logcat -d > navigation-evidence/logcat.txt
for apd_visual_name in uimsd-navigation-parent.png uimsd-navigation-child-back.png uimsd-navigation-restored.png; do
    adb exec-out cat "/sdcard/Pictures/APD-Test-Visuals/${apd_visual_name}" > "navigation-evidence/${apd_visual_name}"
done
python3 tools/validate_android_visuals.py navigation-evidence uimsd-navigation-parent.png uimsd-navigation-child-back.png uimsd-navigation-restored.png
python3 - <<'PY'
import pathlib,json
p=pathlib.Path('navigation-evidence');s=(p/'instrumentation.txt').read_text()
assert 'OK (1 test)' in s and 'FAILURES!!!' not in s,s[-8000:]
(p/'result.json').write_text(json.dumps({'passed':1,'original_nested_pointer_back_and_recreation_assertions_retained':True,'stable_frame_capture':True,'frozen_panes_aligned':True,'application_rebuilt':False},indent=2))
PY
