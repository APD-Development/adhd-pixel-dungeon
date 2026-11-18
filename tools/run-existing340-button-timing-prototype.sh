#!/usr/bin/env bash
set -u
mkdir -p button-timing-prototype
cp acceptance-source.json button-timing-prototype/
adb shell wm size 1216x2688
adb shell wm density 442
adb logcat -c
adb logcat -v time > button-timing-prototype/logcat.txt &
adb install -r application/*.apk || exit 1
adb install -r android/build/outputs/apk/androidTest/debug/*-androidTest.apk || exit 1
adb shell am instrument -w -r -e class com.shatteredpixel.shatteredpixeldungeon.android.ButtonTimingPrototypeDeviceTest com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner > button-timing-prototype/instrumentation.txt 2>&1
cat button-timing-prototype/instrumentation.txt
for n in main-title-layout.png main-title-hidden.png main-title-restored.png options-confirmations.png; do
 adb exec-out cat "/sdcard/Pictures/APD-Test-Visuals/$n" > "button-timing-prototype/$n" 2>/dev/null || true
done
python3 - <<'PY'
import pathlib,re,json
from PIL import Image
root=pathlib.Path('button-timing-prototype');s=(root/'instrumentation.txt').read_text()
codes=re.findall(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)',s)
passed='OK (1 test)' in s and 'FAILURES!!!' not in s and codes==['1','0']
images={}
for name in ('main-title-layout.png','main-title-hidden.png','main-title-restored.png','options-confirmations.png'):
 try:
  with Image.open(root/name) as im:images[name]=im.size;im.verify()
 except Exception:passed=False
(root/'result.json').write_text(json.dumps(dict(diagnostic_only=True,shipped_apk_acceptance=False,candidate_press_timing_injected=True,input_handler_replaced=False,instrumentation_passed=passed,terminal_codes=codes,images=images),indent=2))
if not passed:raise SystemExit('Pre-DOWN timing prototype failed; no APK acceptance or publication')
PY
