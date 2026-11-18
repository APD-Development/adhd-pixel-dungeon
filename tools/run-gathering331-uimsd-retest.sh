#!/usr/bin/env bash
set -euo pipefail
mkdir -p uimsd-retest
cp acceptance-source.json uimsd-retest/
adb logcat -c
adb logcat -v time > uimsd-logcat.txt &
adb install -r application/*.apk
adb install -r android/build/outputs/apk/androidTest/debug/*-androidTest.apk
adb shell am instrument -w -r -e class 'com.shatteredpixel.shatteredpixeldungeon.android.AndroidBackDeviceTest#warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms' com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner > uimsd-instrumentation.txt 2>&1
cat uimsd-instrumentation.txt
python3 - <<'PY'
import pathlib,re,xml.etree.ElementTree as E
s=pathlib.Path('uimsd-instrumentation.txt').read_text()
assert 'OK (1 test)' in s and 'FAILURES!!!' not in s,s
assert re.findall(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)',s)==['1','0'],s
assert 'warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms' in s
root=E.Element('testsuite',name='ExactTEST331UimsdRetest',tests='1',failures='0',errors='0',skipped='0')
E.SubElement(root,'testcase',classname='com.shatteredpixel.shatteredpixeldungeon.android.AndroidBackDeviceTest',name='warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms')
E.ElementTree(root).write('uimsd-retest/TEST-exact-uimsd.xml',encoding='utf-8',xml_declaration=True)
PY
adb exec-out screencap -p > uimsd-retest/final-screen.png
