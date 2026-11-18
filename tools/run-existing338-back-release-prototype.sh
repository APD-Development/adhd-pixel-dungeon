#!/usr/bin/env bash
set -u
mkdir -p back-release-prototype
cp acceptance-source.json back-release-prototype/
adb shell wm size 320x640
adb shell wm density 160
adb logcat -c
adb logcat -v time > back-release-prototype/logcat.txt &
adb install -r application/*.apk || exit 1
adb install -r android/build/outputs/apk/androidTest/debug/*-androidTest.apk || exit 1
adb shell am instrument -w -r -e class com.shatteredpixel.shatteredpixeldungeon.android.BackReleasePrototypeDeviceTest com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner > back-release-prototype/instrumentation.txt 2>&1
cat back-release-prototype/instrumentation.txt
python3 - <<'PY'
import pathlib,re,json
s=pathlib.Path('back-release-prototype/instrumentation.txt').read_text()
codes=re.findall(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)',s)
passed='OK (1 test)' in s and 'FAILURES!!!' not in s and codes==['1','0']
pathlib.Path('back-release-prototype/result.json').write_text(json.dumps(dict(diagnostic_only=True,timing_acceptance=False,shipped_apk_acceptance=False,candidate_handler_injected=True,instrumentation_passed=passed,terminal_codes=codes),indent=2))
if not passed:raise SystemExit('Proposed Back release regression failed; profiling evidence retained, not accepted')
PY
