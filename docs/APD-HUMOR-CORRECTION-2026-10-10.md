# APD humor policy correction — October 10, 2026

Owner clarification replaces the earlier requirement that unknown/unavailable age always force Clean. Verified adults default to Adult unless they explicitly choose Clean; verified underage players receive Clean regardless of stored preference. Unknown/unavailable age defaults Clean but allows manual Adult selection. A checkbox is a preference, never evidence of age.

## Root cause and implementation
The previous policy returned UNAVAILABLE because no age provider exists, then blocked both reading and enabling the Adult preference. Thus nobody could enable Adult in this test build. AgeEligibility now distinguishes a verified underage prohibition from absent verification and supplies the verified-adult default. SPDSettings retains the same preference key, respects explicit choices and applies the policy. APDSettingsPane rejects only verified underage eligibility and no longer reports that unavailable verification locks the switch.

The title annotation itself is unchanged: the locked message appears before unlock; a demo unlock hides that message; ten real completed runs queue the Clean/Adult arrow; opening UIMSD retires that arrow. Missing annotation in an owner screenshot does not prove a layout bug or establish saved unlock flags.

## Verification
Local JDK compilation of the changed application classes succeeded. 28 targeted JUnit tests passed in 1.341 seconds: age policy, actual disk-backed Adult/Clean preference restoration, real settings-to-title message routing, unlock persistence, and existing humor isolation/Grudge controls. Test preferences are isolated and restored. A new Android regression sends real pointer events to enable Adult, reopens settings to verify the checked control, disables it, and reopens to verify Clean. Android test is committed for CI, not yet passed. Full suite, Android build and emulator verification are pending at this checkpoint.

## Scope and potential regressions
No save schema, statistics, gameplay mechanics, identification, RNG, signing or app ID changes. Existing stored Adult=true choices now take effect without a provider (intentional behavior change). Stored Clean=false overrides an adult default when a provider eventually reports adulthood. Actual age verification is NOT implemented or claimed complete, and no legal protection is claimed. Future trusted-provider integration must supply verified states accurately; the current provider remains explicitly UNAVAILABLE. Physical-phone testing remains required.

## Artifact and work status
Last actually delivered/verified application remains TEST22, source 6ffd9668b853bbb94922888e6880611aebe1a2f8, private tag apd-codex-test-22. This correction is not in that APK. The separate Warrior framework checkpoint 38f761c067190345b66e307628799f22938a6dcc passed its focused CI; fixture tests are not real dungeon runs. The actual game adapter and 100-run study remain unfinished. Pending local hotbar, animation, healing/drop, crystal-room and weapon changes are not silently included in this correction.

Work is evidenced by the compilation/test command records and real GitHub commits/runs. Exact long-term active/idle durations remain unavailable; no overnight execution or zero downtime is claimed.
