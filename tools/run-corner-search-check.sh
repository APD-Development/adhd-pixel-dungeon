#!/usr/bin/env bash
set -euo pipefail
adb shell am instrument -w -r -e class com.shatteredpixel.shatteredpixeldungeon.android.QolItemFeedbackDeviceTest com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner | tee corner-instrumentation.txt
python3 -c "import pathlib; assert 'OK (1 test)' in pathlib.Path('corner-instrumentation.txt').read_text()"
mkdir -p codex-visual-evidence
for name in qol-dew-settings.png qol-force-weapon.png qol-sharpshooting-weapon.png qol-searched-wall.png qol-offline-license.png; do
  adb exec-out cat "/sdcard/Pictures/APD-Test-Visuals/$name" > "codex-visual-evidence/$name"
done
python3 tools/validate_android_visuals.py codex-visual-evidence qol-dew-settings.png qol-force-weapon.png qol-sharpshooting-weapon.png qol-searched-wall.png qol-offline-license.png
