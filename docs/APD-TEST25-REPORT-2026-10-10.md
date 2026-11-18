# APD private TEST25 development report — October 10, 2026
## Change since TEST24
The Community-selected content preview incorrectly told players about private Cleric lore, the creator's vendetta and a hidden achievement. The owner confirmed the disclosure with a screenshot of TEST24. Internal developer restrictions had been copied into player-facing explanatory text; those restrictions did not authorize announcing secret or unreleased content.

Removed the entire disclosure sentence in CommunityPreview.java. Winning ideas still enter development, and the approximate two-month implementation goal still does not discard a winner. No other Community page, navigation, creator authority, gameplay rule, Cleric restriction, age policy, turn timing or refresh-rate setting changed.

CommunityPreviewTest.java replaces the old assertion that wrongly required the disclosure and adds a permanent regression checking all preview titles, explanations, availability text and shared comments against exposing these private topics. Private repository handoff keeps the creator restrictions for developers.

Application source: 42aa5ff0a332eab3afe12f036c9beeeb1bfb5e71 (approved Codex branch; real parent 7d28f7e48e53f8e42bca1c1fb5be9d4fefeb25e8). TEST24 application source remains f1ad8657250defd4b97cf422fa222d6c6c1f7329; TEST24 release and APK are preserved. Main and Claude branches were not modified.

## Verification
Local compilation succeeded. Three targeted Community/title-entry test suites: 15 tests, zero failures, 0.073 seconds. An initial invocation used incorrect packages for two test classes and failed class lookup; the corrected invocation passed. No test failure was hidden.

The appended verification section records actual full core tests, Android debug build, the six API34 emulator checks, APK checksum, matching signing certificate, application ID and source/run information. These checks must all succeed before private publication. An emulator check explores all eight Community pages and returns without stacking windows; the new core check verifies the disclosure is absent from every supplied visible preview string.

Automated tests do not prove physical RedMagic behavior. Check the Community-selected content page on the phone after installing over the existing APD test app. The removed wording must be absent, the rest of the page readable, and Back must return normally. Existing phone saves and statistics must remain visible.

## Regressions and limits
The edit shortens one informational paragraph. It changes no save schema, statistics recording, item rules, RNG, account system or server connectivity. No new regression was observed in the targeted tests; broader results are given by the actual verification appendix. There is no claim of a bug-free game, complete floor-to-victory testing or physical-device verification.

Community remains an honest offline preview, with no live submissions, votes, threaded comments, run reconstruction, mod downloads or backend activation. Clans remain omitted. Public availability dates and private tester schedules are not advertised to players.

## Other work during verification — not included in TEST25
The pending guaranteed enemy-drop identification work gained two real integration regressions. One executes the real normal Swarm/Bat/Necromancer loot path, checks exactly one potion placed on the floor and identification before pickup. The other executes the real Lucky bonus-loot path on a Swarm, checks its healing potion remains unknown, and distinguishes bonus loot from guaranteed normal loot. Together with existing appearance, exotic, RNG/chance and source-hook checks: seven local tests passed in 0.787 seconds; the combined loot plus generation-provenance/safety run passed all 21 tests in 1.697 seconds. These checks also confirm normal drop limits increment once, while Lucky bonus loot leaves the normal limit untouched.

The first normal-loot integration fixture lacked a Game instance and failed at the headless Level.drop scene check. Corrected the test fixture by supplying a no-render Game shell that preserves scene-class/platform state, and restoring the previous Game instance and Camera. No production scene shortcut or drop-rate change was used. This integration work and the associated identification implementation remain unpublished local work, not part of this APK.

The real 100-run Warrior simulator adapter is still unfinished; no 100 real gameplay simulations or healing-benefit percentages are claimed. Category hotbar, Cleric multi-spell shortcuts, healing-threshold confirmation, compact weapon information, animation-speed options and crystal-room changes remain pending and are not included. TEST24 Community and humor functionality is retained, not rebuilt.

## Execution and timing
The prior handoff recorded TEST24 publication at 18:42:59 UTC / 2:42:59 p.m. New York. TEST25 follows a new owner phone report; it is not a fabricated daily change. Targeted correction work and tests occurred during this active turn on October 10. The actual CI start/end timestamps are appended automatically. CI elapsed time is not active human-equivalent coding time.

Full active development time, historical overnight idle time and longest prior inactivity cannot be recovered from commits alone and remain unknown. No continuous overnight execution is claimed. One separately scheduled GitHub inspection/probe commit was demonstrated (7ead4a6e4891b78314e2f05ed69c15f889260a43); unattended coding/building and future repeated operation have not yet been demonstrated.

No new paid service, API dependency, billing change or force push. CI uses the owner's existing included GitHub allocation. Preserve application ID and signing key, previous APK assets, saves and statistics. Never uninstall or clear data to install this test APK.
