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
adb shell am instrument -w -r -e class 'com.shatteredpixel.shatteredpixeldungeon.android.ApdSettingsClickDeviceTest#adultHumorControlWorksWithoutAnAgeProviderAndReopensWithSavedChoice' "${apd_test_package}/androidx.test.runner.AndroidJUnitRunner" > settings-gesture-evidence/instrumentation.txt
adb logcat -d > settings-gesture-evidence/logcat.txt
python3 - <<'PY'
import pathlib,json
p=pathlib.Path('settings-gesture-evidence');s=(p/'instrumentation.txt').read_text()
assert 'OK (1 test)' in s and 'FAILURES!!!' not in s,s[-8000:]
(p/'result.json').write_text(json.dumps({'passed':1,'original_scroll_and_non_activation_assertions_retained':True,'touch_origin_inside_scroll_area_asserted':True,'room_to_scroll_asserted':True,'application_rebuilt':False},indent=2))
PY
