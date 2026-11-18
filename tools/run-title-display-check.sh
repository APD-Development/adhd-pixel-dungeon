#!/usr/bin/env bash
set -euo pipefail
adb shell wm density "$2"
adb shell wm size "$1"
apd_mode="${3:-title}"
mkdir -p title-evidence
adb shell wm size > title-evidence/display.txt
adb shell wm density >> title-evidence/display.txt
if [ "${APD_ENFORCE_DISPLAY:-false}" = true ]; then
  python3 - "$1" <<'PY'
import pathlib,re,sys
text=pathlib.Path("title-evidence/display.txt").read_text()
sizes=re.findall(r"(?:Physical|Override) size: (\d+x\d+)",text)
assert sizes and sizes[-1]==sys.argv[1],("Requested/effective display differ",sys.argv[1],text)
PY
fi
cp acceptance-source.json title-evidence/
apd_sdk="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
if [ "$apd_sdk" -ge 36 ]; then
  # The Google API 36 image can still be draining first-boot broadcasts after
  # sys.boot_completed. Do not begin real Back delivery checks in that state.
  timeout 180 adb shell am wait-for-broadcast-idle
fi
adb logcat -c
adb logcat -v time > title-logcat.txt &
if [ "$apd_mode" = full ]; then
  apd_classes='com.shatteredpixel.shatteredpixeldungeon.android.UimsdRestoreDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.AndroidBackDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.PlayRunDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.RunSetupDifficultyDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.ApdSettingsClickDeviceTest#adultHumorControlWorksWithoutAnAgeProviderAndReopensWithSavedChoice,com.shatteredpixel.shatteredpixeldungeon.android.ApdSettingsClickDeviceTest#grudgeControlsRespondToActualTouches,com.shatteredpixel.shatteredpixeldungeon.android.ApdSettingsClickDeviceTest#communityMenuOpensEveryPreviewAndReturnsWithoutStackingWindows,com.shatteredpixel.shatteredpixeldungeon.android.AboutDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.UimsdScreensDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.UimsdResourcesDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.ChangesVisualDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.ReleaseNotesNavigationDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.CustomRulesNoticeDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.TopScoresDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.CustomScoreDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.TitleMenuLayoutDeviceTest'
  apd_images='boss-key-no-promotion.png victory-no-promotion.png options-confirmations.png waterskin-confirm.png known-trap-confirm.png main-title-layout.png main-title-hidden.png main-title-restored.png hotbar-controls.png daily-earned-score.png daily-earned-score-scrolled.png custom-scoring-vision.png custom-scoring-vision-narrow.png custom-rules-live-drag.png custom-rules-live-summary.png healing-conversion-setting.png cleric-first-run.png cleric-empty-slot-picker.png cleric-four-slot-touch.png apd-changes.png apd-technical-changes.png release-notes-navigation.png release-notes-reopened.png spd-changes-reference.png apd-build-22.png apd-build-37.png apd-codex-test-67.png tiles-original-2.png tiles-enhanced-2.png tiles-original-4.png tiles-enhanced-4.png stairs-shortcut.png stairs-highlight.png uimsd-empty-home.png uimsd-home.png uimsd-demo-info.png uimsd-text-values.png uimsd-sort-keys.png chalice-preview.png blacksmith-rewards.png blacksmith-reward-detail.png healing-confirmation.png healing-threshold-settings.png metamorph-five-choices.png metamorph-choice-detail.png custom-rules-notice.png custom-rules-notice-narrow.png custom-rules-notice-narrow-scrolled.png custom-rules-equipment.png custom-rules-touch-gestures.png top-scores-main-menu.png top-scores-rankings.png custom-score-neutral.png custom-score-pending.png custom-score-unknown.png custom-score-narrow.png custom-score-narrow-before.png'
  apd_methods=38
elif [ "$apd_mode" = captures ]; then
  apd_classes='com.shatteredpixel.shatteredpixeldungeon.android.PlayRunDeviceTest#aRunCanBePlayedSavedContinuedAndEndedWithItsHistoryRecorded,com.shatteredpixel.shatteredpixeldungeon.android.CustomScoreDeviceTest#dailyCalendarShowsFrozenLongEarnedScoreWithoutOverlappingDates,com.shatteredpixel.shatteredpixeldungeon.android.CustomRulesNoticeDeviceTest#scoringVisionPopupOpensScrollsAndBackPreservesDraftOnSmallScreens'
  apd_images='hotbar-controls.png daily-earned-score.png daily-earned-score-scrolled.png custom-scoring-vision.png custom-scoring-vision-narrow.png'
  apd_methods=3
elif [ "$apd_mode" = scale ]; then
  apd_classes='com.shatteredpixel.shatteredpixeldungeon.android.TitleMenuLayoutDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.ScaleChangeDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.QolStableCaptureDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.HealingSettingsDeviceTest'
  apd_images='main-title-layout.png main-title-hidden.png main-title-restored.png options-confirmations.png scale-phone-seven.png scale-phone-six.png'
  apd_images="$apd_images qol-doorless-floor1.png qol-door-ineligible-bookshelf.png qol-door-ineligible-decoration.png qol-dew-settings.png qol-force-weapon.png qol-sharpshooting-weapon.png qol-searched-wall.png qol-searched-wall-color.png healing-settings-toggle-on.png healing-settings-toggle-off.png healing-settings-entry.png healing-settings-editor.png healing-settings-confirm.png qol-offline-license.png"
  apd_methods=6
else
  apd_classes=com.shatteredpixel.shatteredpixeldungeon.android.TitleMenuLayoutDeviceTest
  apd_images='main-title-layout.png main-title-hidden.png main-title-restored.png options-confirmations.png'
  apd_methods=1
fi
export APD_EXPECTED_METHODS="$apd_methods" APD_EXPECTED_IMAGES="$apd_images"
apd_collect() {
  for apd_image in $apd_images; do
    adb exec-out cat "/sdcard/Pictures/APD-Test-Visuals/$apd_image" > "title-evidence/$apd_image" 2>/dev/null || rm -f "title-evidence/$apd_image"
  done
  adb exec-out screencap -p > title-evidence/final-screen.png || true
}
trap apd_collect EXIT
adb install -r application/*.apk
adb install -r android/build/outputs/apk/androidTest/debug/*-androidTest.apk
adb shell am instrument -w -r -e class "$apd_classes" com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner > title-instrumentation.txt 2>&1
cat title-instrumentation.txt
apd_collect
python3 tools/validate_android_visuals.py title-evidence $apd_images
python3 tools/record-native-confirmation-results.py
