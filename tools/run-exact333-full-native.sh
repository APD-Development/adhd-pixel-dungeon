#!/usr/bin/env bash
set -u
apd_test_status=0
mkdir -p native333-evidence
cp acceptance-source.json native333-evidence/
adb logcat -c
adb logcat -v time > native333-evidence/logcat.txt &
adb install -r application/*.apk || exit $?
adb install -r android/build/outputs/apk/androidTest/debug/*-androidTest.apk || exit $?
timeout --signal=TERM --kill-after=30 1080 adb shell am instrument -w -r -e class 'com.shatteredpixel.shatteredpixeldungeon.android.UimsdRestoreDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.UimsdDeviceTimingTest,com.shatteredpixel.shatteredpixeldungeon.android.AndroidBackDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.PlayRunDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.RunSetupDifficultyDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.ApdSettingsClickDeviceTest#adultHumorControlWorksWithoutAnAgeProviderAndReopensWithSavedChoice,com.shatteredpixel.shatteredpixeldungeon.android.ApdSettingsClickDeviceTest#grudgeControlsRespondToActualTouches,com.shatteredpixel.shatteredpixeldungeon.android.ApdSettingsClickDeviceTest#communityMenuOpensEveryPreviewAndReturnsWithoutStackingWindows,com.shatteredpixel.shatteredpixeldungeon.android.ApdSettingsClickDeviceTest#categorizedSettingsPreserveControlsAndRespondOnSmallScreens,com.shatteredpixel.shatteredpixeldungeon.android.AboutDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.UimsdScreensDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.UimsdResourcesDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.ChangesVisualDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.ReleaseNotesNavigationDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.CustomRulesNoticeDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.TopScoresDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.CustomScoreDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.TitleMenuLayoutDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.ClassSaveSlotsDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.PileExaminationDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.UimsdSearchDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.UimsdColumnsModalDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.UimsdChartDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.UimsdResearchDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.LootScrollDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.DiagnosticReportDeviceTest,com.shatteredpixel.shatteredpixeldungeon.android.ScaleChangeDeviceTest#nativeTapQueuedDuringTitleBuildIsDiscardedButFreshTapWorks,com.shatteredpixel.shatteredpixeldungeon.android.ProgressionTooltipsDeviceTest' com.charltonchris29.adhdpixeldungeon.indev.test/androidx.test.runner.AndroidJUnitRunner > native333-evidence/instrumentation.txt 2>&1 || apd_test_status=$?
cat native333-evidence/instrumentation.txt
python3 - <<'PY' || apd_test_status=$?
import pathlib,xml.etree.ElementTree as E
s=pathlib.Path('native333-evidence/instrumentation.txt').read_text()
assert 'OK (66 tests)' in s and 'FAILURES!!!' not in s,s[-10000:]
fields={};cases=[]
for line in s.splitlines():
 if line.startswith('INSTRUMENTATION_STATUS: '):
  field=line[len('INSTRUMENTATION_STATUS: '):]
  if '=' in field:
   key,value=field.split('=',1);fields[key]=value
 elif line.startswith('INSTRUMENTATION_STATUS_CODE: '):
  code=int(line.split(':',1)[1].strip())
  assert code in (0,1),(code,fields)
  if code==0:cases.append((fields['class'],fields['test']))
  fields={}
assert len(cases)==len(set(cases))==66
assert any(n=='multipleColumnTapsRemainAboveTableUntilDone' for c,n in cases)
assert any(n=='evidenceBackRestoresEachTableAndSurvivesSceneRecreation' for c,n in cases)
root=E.Element('testsuite',name='ExactTEST333FullNative',tests='66',failures='0',errors='0',skipped='0')
for cls,name in cases:E.SubElement(root,'testcase',classname=cls,name=name)
E.ElementTree(root).write('native333-evidence/TEST-native-exact333.xml',encoding='utf-8',xml_declaration=True)
PY
mkdir -p codex-visual-evidence
for apd_visual_name in apd-settings-categories.png apd-settings-gameplay.png apd-settings-rules.png apd-settings-hotbar.png apd-settings-visibility.png apd-settings-history.png apd-settings-advanced.png apd-settings-narrow.png uimsd-navigation-parent.png uimsd-navigation-child-back.png uimsd-navigation-restored.png uimsd-equipment-window.png uimsd-damage-recovery-window.png uimsd-death-context.png uimsd-run-dna.png uimsd-research-evidence.png uimsd-luck-lab.png uimsd-discoveries.png uimsd-investigation.png uimsd-chart-values.png uimsd-chart-counts.png hotbar-controls.png daily-earned-score.png daily-earned-score-scrolled.png custom-scoring-vision.png custom-scoring-vision-narrow.png boss-key-no-promotion.png victory-no-promotion.png options-confirmations.png waterskin-confirm.png known-trap-confirm.png main-title-layout.png main-title-hidden.png main-title-restored.png custom-rules-live-drag.png custom-rules-live-summary.png healing-conversion-setting.png cleric-unflagged-start.png cleric-first-run.png cleric-empty-slot-picker.png cleric-four-slot-touch.png apd-changes.png apd-technical-changes.png release-notes-navigation.png release-notes-reopened.png spd-changes-reference.png apd-build-22.png apd-build-37.png apd-codex-test-67.png tiles-original-2.png tiles-enhanced-2.png tiles-original-4.png tiles-enhanced-4.png stairs-shortcut.png stairs-highlight.png uimsd-empty-home.png uimsd-home.png uimsd-demo-info.png uimsd-text-values.png uimsd-sort-keys.png chalice-preview.png blacksmith-rewards.png blacksmith-reward-detail.png healing-confirmation.png healing-threshold-settings.png metamorph-five-choices.png metamorph-choice-detail.png custom-rules-notice.png custom-rules-notice-narrow.png custom-rules-notice-narrow-scrolled.png custom-rules-equipment.png custom-rules-touch-gestures.png top-scores-main-menu.png top-scores-rankings.png custom-score-neutral.png custom-score-pending.png custom-score-unknown.png custom-score-narrow.png custom-score-narrow-before.png uimsd-experiment-initial.png uimsd-experiment-future.png uimsd-saved-question.png loot-scroll-gather-description.png loot-scroll-gather-result.png loot-scroll-forward-description.png loot-scroll-pending-after-continue.png diagnostic-report-button.png diagnostic-report-saved.png resource-strength-current.png resource-upgrade-current.png resource-strength-legacy.png resource-upgrade-legacy.png; do
    if ! adb exec-out cat "/sdcard/Pictures/APD-Test-Visuals/${apd_visual_name}" > "codex-visual-evidence/${apd_visual_name}"; then
        rm -f "codex-visual-evidence/${apd_visual_name}"
        if [ "$apd_test_status" -eq 0 ]; then apd_test_status=1; fi
    fi
done
if ! python3 tools/validate_android_visuals.py codex-visual-evidence apd-settings-categories.png apd-settings-gameplay.png apd-settings-rules.png apd-settings-hotbar.png apd-settings-visibility.png apd-settings-history.png apd-settings-advanced.png apd-settings-narrow.png uimsd-navigation-parent.png uimsd-navigation-child-back.png uimsd-navigation-restored.png uimsd-equipment-window.png uimsd-damage-recovery-window.png uimsd-death-context.png uimsd-run-dna.png uimsd-research-evidence.png uimsd-luck-lab.png uimsd-discoveries.png uimsd-investigation.png uimsd-chart-values.png uimsd-chart-counts.png hotbar-controls.png daily-earned-score.png daily-earned-score-scrolled.png custom-scoring-vision.png custom-scoring-vision-narrow.png boss-key-no-promotion.png victory-no-promotion.png options-confirmations.png waterskin-confirm.png known-trap-confirm.png main-title-layout.png main-title-hidden.png main-title-restored.png custom-rules-live-drag.png custom-rules-live-summary.png healing-conversion-setting.png cleric-unflagged-start.png cleric-first-run.png cleric-empty-slot-picker.png cleric-four-slot-touch.png apd-changes.png apd-technical-changes.png release-notes-navigation.png release-notes-reopened.png spd-changes-reference.png apd-build-22.png apd-build-37.png apd-codex-test-67.png tiles-original-2.png tiles-enhanced-2.png tiles-original-4.png tiles-enhanced-4.png stairs-shortcut.png stairs-highlight.png uimsd-empty-home.png uimsd-home.png uimsd-demo-info.png uimsd-text-values.png uimsd-sort-keys.png chalice-preview.png blacksmith-rewards.png blacksmith-reward-detail.png healing-confirmation.png healing-threshold-settings.png metamorph-five-choices.png metamorph-choice-detail.png custom-rules-notice.png custom-rules-notice-narrow.png custom-rules-notice-narrow-scrolled.png custom-rules-equipment.png custom-rules-touch-gestures.png top-scores-main-menu.png top-scores-rankings.png custom-score-neutral.png custom-score-pending.png custom-score-unknown.png custom-score-narrow.png custom-score-narrow-before.png uimsd-experiment-initial.png uimsd-experiment-future.png uimsd-saved-question.png loot-scroll-gather-description.png loot-scroll-gather-result.png loot-scroll-forward-description.png loot-scroll-pending-after-continue.png diagnostic-report-button.png diagnostic-report-saved.png resource-strength-current.png resource-upgrade-current.png resource-strength-legacy.png resource-upgrade-legacy.png; then apd_test_status=1; fi
cp -R codex-visual-evidence native333-evidence/visuals
exit "$apd_test_status"





