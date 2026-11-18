# APD UIMSD Home scope acceptance — 2026-10-17

## Result

The current `UimsdHome.java` blob `71fd5b3b626ccc7cf388445c34ad7d57677a86b7` was read completely with its Catalog, DatasetStore and QueryEngine presentation paths. No production defect was confirmed.

Test-only commit `914586aa83175b54528b2bbf59a2db71a4663bd7` adds `homeScopeExcludesUnfinishedRunsAndCountsOtherFinishedRulesExactly` to `UimsdHomePresentationTest`. It independently constructs:

- one finished run under the current rules;
- one unfinished run under the current rules; and
- one finished run under another ruleset.

The retained contract requires current-rules Home to count one run and disclose one finished run elsewhere, all-rules Home to count exactly the two finished runs, the visible `Finished runs` line to remain exact, and the unfinished Mage identity to remain absent from Home text.

## Full core evidence

- Request commit: `01a32c305da5f97747c5d73c32762e80dd27f8c6`
- Workflow run: `39003588552`
- Started: `2026-10-17T04:42:44Z`
- Completed: `2026-10-17T04:44:37Z`
- Result: success
- JUnit XML: 350 suites, 1,770 tests, 0 failures, 0 errors, 0 skipped
- `UimsdHomePresentationTest`: 2 methods passed
- New method duration: 0.008 seconds
- Artifact: `12089109846`, 826,513 bytes
- Artifact digest and independently downloaded ZIP SHA-256: `98c9e31f6bc7a3ea9810bfc952026548983877dfac6266c26310c53e7cea710b`

## Change boundaries

This is test and documentation only. Production application source, `docs/APD-CODEX-BUILD-REQUEST.txt`, APKs, signing, application ID, saves, permanent statistics, raw history, all 18 Custom Dungeon Rules, v2.6R scoring, original scores, gameplay, turns and RNG are unchanged.

TEST264 remains the latest verified/staged private candidate. TEST262 remains the latest actually published owner APK. No physical-phone, publication, or tester-distribution claim is made.

The exact current-blob UIMSD review count is now 100. The broader audit and gameplay acceptance backlog remain open.
