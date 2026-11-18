# APD hotbar lunch-test follow-up — TEST41

## Changes
- Custom hotbar favorites display the actual carried item icon, quantity and item name, making different favorites distinguishable.
- Hover text follows the selected hotbar mode rather than showing a Classic assignment or generic slot name.
- Unavailable favorites retain their saved preferences and do not reveal unidentified potion identities.
- Category presets, freely arranged Custom categories or known item types, optional food in Healing, and Classic SPD remain available in APD Settings → Hotbar layout. All six positions are reachable on narrow screens using the existing swap control.
- Opening categories and configuring favorites do not automatically use an item or spend a gameplay turn. Classic assignments are preserved.

## Root cause
Custom favorites stored an item type but deliberately rendered the same generic Item label for every type. The original hover callback continued reading Classic slots even when category mode was active. The fix obtains only an eligible currently carried item through the existing category inventory filter and uses its normal icon/name.

## Tests and verification
23 targeted hotbar/settings/tour tests passed locally in 0.437 seconds. Two new permanent regressions cover actual favorite identity after inventory removal and retaining an unknown Healing potion without revealing it in a later run.
The Android gameplay integration test now checks favorite item display, name and non-consumption, preservation of Classic assignments, cleanup of category windows and restoration of saved custom preferences.
Full required verification passed in workflow 38103226643: 1035 core tests and 11 API34 emulator tests, with zero failures, errors or skips; Android debug compilation succeeded. Native XML confirms the actual favorite display/name and play/save/resume integration test passed. Final verified source: e9f1515a52e219282772a590419e215c9a8139f1. Signing, checksum and application ID are checked directly against TEST39 before publication and recorded in the evidence appendix.
Source commit: 1f57136a7cb9e6c55aab48da3d23f44eaa0efc3e, real parent 33708e2cc61298ae1c4cc6fe504e0f92de54648d, approved Codex branch. Main and Claude's branch were not overwritten.

## Potential regressions and phone checks
Specific favorites now use an item icon instead of the generic text label; check readability and quantity updates with several favorites and after consuming/removing an item.
Retest categories, reordered and duplicate Custom slots, saved layout after restart, Classic assignments, narrow toolbar swapping and attack/search/wait separation.
No changes to item effects, turn costs, RNG, saves, application ID or signing identity are intended.
Automated and emulator testing do not establish physical-phone acceptance. Android17 Pixel/Pixel Buds Pro2 interaction remains unverified; TEST39 already included navigation inset repairs.
Four separately saved Cleric quick-cast shortcuts remain unfinished; the existing full Cleric spell picker is preserved. This report does not claim that feature is complete.
No servers, fake voting, paid advantages or unrelated gameplay balance changes were enabled.

## Previous verified build and rollback
TEST39 remains the verified playable fallback: source 475c80c22285978bf9eefb682f7a19bd32d994e5, 1033 core tests and 11 API34 emulator tests passed; SHA256 fdba6ccb7ec634e86ec64414e6e92a6bfeff3584fd4931cc59066326a5e7e7ee.
Preserve TEST39 and earlier private release assets. Install an update over the existing application; never uninstall or clear player data.

## Honest timing
Local targeted tests completed at approximately 2026-10-11 01:46 UTC (October10 9:46PM EDT). Follow-up source was committed and pushed around 01:48 UTC. Exact candidate build and publication timestamps are recorded by GitHub Actions and the verified evidence appendix.
No reliable total active-versus-idle meter exists for this session. No continuous overnight execution or zero downtime is claimed.

## Verification harness correction
Before publication, source inspection found that the Android toolbar exposes QuickslotTool wrappers, rather than QuickSlotButton objects directly. The new assertion was corrected to inspect the actual inner button in commit e9f1515a52e219282772a590419e215c9a8139f1. No application behavior changed. The preceding candidate is superseded and is not claimed verified or delivered.

The superseded TEST40 workflow 38103051244 completed core tests/debug compilation successfully; its Android and publication jobs were canceled by the next approved-branch verification push. No TEST40 release was published. Final TEST41 evidence is required independently.

Final source workflow ran 2026-10-11T01:51:20Z–02:02:26Z (October10 9:51:20–10:02:26PM EDT). This is total CI elapsed time, not a measurement of coding time. Nine explicit CI timer intervals totaled 525 seconds (one 45-second interval and eight 60-second intervals); some source review and report preparation overlapped these timers. This is recorded waiting, not total session downtime. Private artifact publication runs separately and its exact completion time is recorded in the handoff.
