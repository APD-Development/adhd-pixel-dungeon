#!/usr/bin/env bash
set -euo pipefail
mkdir -p resource-evidence
cp acceptance-source.json resource-evidence/
adb shell wm size 320x640
adb shell wm density 160
adb logcat -c
adb logcat -v time > resource-evidence/logcat.txt &
adb install -r application/*.apk
adb install -r android/build/outputs/apk/androidTest/debug/*-androidTest.apk
adb shell am instrument -w -r -e class com.shatteredpixel.shatteredpixeldungeon.android.ProgressionTooltipsDeviceTest com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner > resource-evidence/instrumentation.txt 2>&1
cat resource-evidence/instrumentation.txt
python3 - <<'PY'
import pathlib,re,xml.etree.ElementTree as E
s=pathlib.Path('resource-evidence/instrumentation.txt').read_text()
assert 'OK (1 test)' in s and 'FAILURES!!!' not in s,s
assert re.findall(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)',s)==['1','0'],s
root=E.Element('testsuite',name='ExactTEST333ResourceTooltips',tests='1',failures='0',errors='0',skipped='0')
E.SubElement(root,'testcase',classname='com.shatteredpixel.shatteredpixeldungeon.android.ProgressionTooltipsDeviceTest',name='resourceTooltipsShowExactPairsAndKeepActionsVisibleForNewAndLegacyHistory')
E.ElementTree(root).write('resource-evidence/TEST-resource-tooltips.xml',encoding='utf-8',xml_declaration=True)
PY
for apd_image in resource-strength-current.png resource-upgrade-current.png resource-strength-legacy.png resource-upgrade-legacy.png; do
 adb exec-out cat "/sdcard/Pictures/APD-Test-Visuals/$apd_image" > "resource-evidence/$apd_image"
done
python3 tools/validate_android_visuals.py resource-evidence resource-strength-current.png resource-upgrade-current.png resource-strength-legacy.png resource-upgrade-legacy.png
