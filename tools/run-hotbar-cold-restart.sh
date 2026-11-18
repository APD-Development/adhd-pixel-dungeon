#!/usr/bin/env bash
set -euo pipefail
apd_aapt="$(find "$ANDROID_HOME/build-tools" -type f -name aapt | sort -V | tail -1)"
apd_test_apk=android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk
apd_package="$("$apd_aapt" dump badging hotbar-restart-evidence/after.apk | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
apd_test_package="$("$apd_aapt" dump badging "$apd_test_apk" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
test -n "$apd_package"
test -n "$apd_test_package"
apd_cold_class=com.shatteredpixel.shatteredpixeldungeon.android.HotbarColdRestartDeviceTest
apd_run_phase() {
    adb logcat -c
    adb shell am instrument -w -r -e apdOldDiskState "${3:-false}" -e class "${apd_cold_class}#$2" "${apd_test_package}/androidx.test.runner.AndroidJUnitRunner" > "hotbar-restart-evidence/$1-instrumentation.txt"
    adb logcat -d > "hotbar-restart-evidence/$1-logcat.txt"
}
adb install -r -d hotbar-restart-evidence/before.apk
adb install -r -d "$apd_test_apk"
# Emulator fixtures only. The actual cold restart below does not clear application data.
adb shell pm clear "$apd_package"
apd_run_phase before-prepare prepareManuallyAssignedSavedRuns
adb shell am force-stop "$apd_package"
apd_run_phase before-restore coldProcessRestoresExistingSavesWithoutReapplyingStartingPreference
adb shell am force-stop "$apd_package"
# Upgrade the same installation with TEST285 saves and preferences still present.
adb install -r -d hotbar-restart-evidence/after.apk
adb install -r -d "$apd_test_apk"
apd_run_phase after-upgrade coldProcessRestoresExistingSavesWithoutReapplyingStartingPreference true
apd_run_phase after-prepare prepareManuallyAssignedSavedRuns
adb shell am force-stop "$apd_package"
apd_run_phase after-restore coldProcessRestoresExistingSavesWithoutReapplyingStartingPreference
adb exec-out cat /sdcard/Pictures/APD-Test-Visuals/hotbar-cold-restored.png > hotbar-restart-evidence/hotbar-cold-restored.png
# Exercise the same categorized settings UI in the regular-player variant.
adb shell am force-stop "$apd_package"
adb install -r -d hotbar-restart-evidence/player.apk
adb install -r -d "$apd_test_apk"
adb shell am instrument -w -r -e class 'com.shatteredpixel.shatteredpixeldungeon.android.ApdSettingsClickDeviceTest#categorizedSettingsPreserveControlsAndRespondOnSmallScreens' "${apd_test_package}/androidx.test.runner.AndroidJUnitRunner" > hotbar-restart-evidence/player-settings-instrumentation.txt
for apd_view in categories gameplay rules hotbar visibility history advanced narrow; do
    adb exec-out cat "/sdcard/Pictures/APD-Test-Visuals/apd-settings-${apd_view}.png" > "hotbar-restart-evidence/player-apd-settings-${apd_view}.png"
done
python3 - <<'PY'
import json,pathlib,zipfile
p=pathlib.Path('hotbar-restart-evidence')
for phase in ('before-prepare','after-upgrade','after-prepare','after-restore','player-settings'):
    s=(p/(phase+'-instrumentation.txt')).read_text()
    assert 'OK (1 test)' in s and 'FAILURES!!!' not in s,(phase,s[-5000:])
before=(p/'before-restore-instrumentation.txt').read_text()
assert 'FAILURES!!!' in before and 'Manual Waterskin and throwing-spike assignments must survive cold restart' in before,before[-5000:]
assert 'scenario=0 expected=' in before and 'scenario=1 expected=' in before,before[-5000:]
with zipfile.ZipFile(p/'player.apk') as player:assert 'assets/apd/features.txt' not in player.namelist()
(p/'result.json').write_text(json.dumps({'baseline_test285_reproduced':True,'upgrade_from_test285_passed':True,'old_disk_state_preserved_on_upgrade':True,'unsaved_test285_edits_cannot_be_recovered':True,'new_save_cold_restart_passed':True,'complete_process_force_stop':True,'application_data_cleared_during_restart':False,'application_rebuilt':False,'regular_player_settings_passed':True,'regular_player_test_feature_asset_absent':True,'physical_phone_verified':False},indent=2))
PY

python3 tools/validate_android_visuals.py hotbar-restart-evidence hotbar-cold-restored.png
python3 tools/validate_android_visuals.py hotbar-restart-evidence player-apd-settings-categories.png player-apd-settings-gameplay.png player-apd-settings-rules.png player-apd-settings-hotbar.png player-apd-settings-visibility.png player-apd-settings-history.png player-apd-settings-advanced.png player-apd-settings-narrow.png
