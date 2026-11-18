#!/usr/bin/env bash
set -u
mkdir -p back-routing-diagnostic
cp acceptance-source.json back-routing-diagnostic/
adb shell wm size 320x640
adb shell wm density 160
adb logcat -c
adb logcat -v time > back-routing-diagnostic/logcat.txt &
adb install -r application/*.apk || exit 1
adb install -r android/build/outputs/apk/androidTest/debug/*-androidTest.apk || exit 1
adb shell am instrument -w -r -e class com.shatteredpixel.shatteredpixeldungeon.android.BackRoutingDiagnosticDeviceTest com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner > back-routing-diagnostic/instrumentation.txt 2>&1
cat back-routing-diagnostic/instrumentation.txt
python3 - <<'PY'
import pathlib,re,json
s=pathlib.Path('back-routing-diagnostic/instrumentation.txt').read_text()
codes=re.findall(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)',s)
passed='OK (1 test)' in s and 'FAILURES!!!' not in s and codes==['1','0']
pathlib.Path('back-routing-diagnostic/result.json').write_text(json.dumps(dict(diagnostic_only=True,timing_acceptance=False,instrumentation_passed=passed,terminal_codes=codes),indent=2))
if not passed:raise SystemExit('Underlying native Back regression failed; profiling evidence retained, not accepted')
PY
