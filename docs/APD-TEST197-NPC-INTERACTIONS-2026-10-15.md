# TEST197 — canonical NPC interaction recording verified

Timestamp: 2026-10-15T06:55:59Z

Source: `364fd4724b79308fb6a13478a211bcd7599b2f7b`  
Run: [38694008236](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38694008236), 06:42:17Z–06:54:46Z

The source milestone is TEST197. The sprint workflow's independent run counter named the compiled artifact `ADHD-Pixel-Dungeon-CODEX-TEST-194.apk`; this distinction is retained rather than relabelled.

## Result

- Core: 1,610 tests in 313 XML suites; 0 failures, 0 errors, 0 skips.
- The retained `NpcInteractionRecordingTest.centralHeroInteractionRecordsAcceptedNpcOnceAndRejectsNoEvent` passed.
- Debug assembly passed.
- Immediate-previous compiled TEST192 APK-history comparison passed with every reviewed path.
- API34: 27 methods; 0 failures, 0 errors, 0 skips.
- Visual: 35 screenshots, all 320×640; full contact sheet reviewed clean. APD Changes shows Codex TEST 194 followed by TEST192/191/190/189.
- APK SHA-256: `8fc77119f6f16193a4fb42ca539b53bbb8d75e26c23c1112a6708b51a2615beb`.

Artifacts:

- Core `11953586184`; ZIP SHA-256 `3f2fdb0c4bde49d7a9162ab83e797d9273e191229a81da64708906d1ae48cb24`.
- APK `11953825841`; ZIP SHA-256 `312b0e8240fe14cb55a989fa0cf045f53f789a4aafa6c3edcc8d4145f1653377`.
- Android `11954147276`; ZIP SHA-256 `8f18ea2bfb5b13600bbb3a6212ebbdba6c64847bee005d89a896473f968ce1df`.

## Scope

Telemetry 1.12 now records each accepted hero/NPC interaction exactly once with only the displayed NPC name and current floor. Rejected interactions emit nothing; the existing `npc.quest` fact remains separate. The earlier lever candidate was not implemented because no such mechanic exists in this source.

No gameplay result, turn, RNG, save, permanent statistics, scoring, custom rules, application identity or signing behavior changed. This is hosted/emulator evidence, not physical-phone or owner-save verification. No APK was distributed; TEST123 remains the last owner-delivered/signature-compared APK.

TEST196 run `38693568938` remains a failed integration-contract run and is not relabelled. Do not rebuild unchanged TEST197 source. The next autonomous canonical recording candidate is shop stock seen, only after reproducing a concrete omission and completing a hidden-information review.
