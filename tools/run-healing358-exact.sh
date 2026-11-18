#!/usr/bin/env bash
# ADHD Pixel Dungeon
# Copyright (C) 2026 Chris Charlton
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>
set -euo pipefail
adb shell settings put global hide_error_dialogs 1
adb shell settings put secure immersive_mode_confirmations confirmed
adb install -r application/ADHD-Pixel-Dungeon-CODEX-TEST-358.apk
adb install -r android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk
if [ "$1" = api34 ]; then
  adb shell wm size 320x640
  adb shell wm density 160
  adb logcat -c
  adb logcat -v time > codex-logcat.txt &
  adb shell am instrument -w -r -e class 'com.shatteredpixel.shatteredpixeldungeon.android.HealingSettingsDeviceTest#actualSettingsChooserSavesThresholdAndGatesRealPotionUse' com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner > existing-apk-instrumentation.txt
  cat existing-apk-instrumentation.txt
  mkdir -p codex-visual-evidence
  adb pull /sdcard/Pictures/APD-Test-Visuals/. codex-visual-evidence/
  python3 -c "import pathlib; assert 'OK (1 test)' in pathlib.Path('existing-apk-instrumentation.txt').read_text()"
else
  bash tools/run-title-display-check.sh 1216x2688 442 scale
fi
