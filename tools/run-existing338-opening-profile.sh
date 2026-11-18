#!/usr/bin/env bash
set -u
mkdir -p opening-profile
cp acceptance-source.json opening-profile/
adb shell wm size 320x640
adb shell wm density 160
adb logcat -c
adb logcat -v time > opening-profile/logcat.txt &
adb install -r application/*.apk || exit 1
adb install -r android/build/outputs/apk/androidTest/debug/*-androidTest.apk || exit 1
adb shell am instrument -w -r -e class com.shatteredpixel.shatteredpixeldungeon.android.UimsdOpeningProfileDeviceTest com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner > opening-profile/instrumentation.txt 2>&1
cat opening-profile/instrumentation.txt
python3 - <<'PY'
import pathlib,re,json
s=pathlib.Path('opening-profile/instrumentation.txt').read_text()
codes=re.findall(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)',s)
passed='OK (1 test)' in s and 'FAILURES!!!' not in s and codes==['1','0']
pathlib.Path('opening-profile/result.json').write_text(json.dumps(dict(diagnostic_only=True,timing_acceptance=False,instrumentation_passed=passed,terminal_codes=codes),indent=2))
if not passed:raise SystemExit('Underlying native opening regression failed; profiling evidence retained, not accepted')
PY
