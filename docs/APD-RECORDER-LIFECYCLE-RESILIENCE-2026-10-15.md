# Recorder lifecycle payload resilience — October 15, 2026

## Reproduced boundary

`RunRecorder` restored logical-run, segment and ruleset bookkeeping from the canonical raw event stream inside one shared exception boundary. A malformed `RUN_START` or `RULESET_CHANGE` payload therefore stopped the scan and hid every later readable lifecycle event from the resumed recorder.

The raw history remained present, but later saves or run-end bookkeeping could be based on stale in-memory ruleset state.

## Bounded correction

- Decode each relevant lifecycle payload in its own failure boundary.
- Continue to later canonical events after one unreadable payload.
- Apply the same isolation while finding an earlier segment header.
- Preserve the original raw bytes, timeline, saves, scores, gameplay, rules, turns and RNG.

One retained core method supplies a malformed `RUN_START` followed by a readable `RULESET_CHANGE`. It must fail on exact TEST231 application source `5134ef6438f9562a37b3f8db727d5d004fa93527` and pass after the correction.

## Acceptance boundary

Pending evidence must show the one intended TEST231 failure, 1,694 corrected core methods, debug assembly, complete immediate-previous TEST231 history, 33 API34 emulator methods and 42 valid screenshots. No automatic release, tester distribution or physical-phone claim is authorized by this batch.

## Actual verification result

The acceptance boundary passed.

- Targeted run: [38841967223](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38841967223). Exact TEST231 application source reproduced exactly one intended failure: the new method expected `new-rules` and restored an empty ruleset. Corrected source passed.
- Full run: [38841967217](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38841967217), 2026-10-16T02:44:25Z–02:57:22Z. It passed 1,694 unique core methods across 334 suites, debug assembly, complete immediate-previous produced TEST231 history, 33 API34 emulator methods and 42 valid 320×640 screenshots with 0 failures/errors/skips.
- APK: `ADHD-Pixel-Dungeon-CODEX-TEST-232.apk`, 38,585,886 bytes, SHA-256 `e6e303276713e6521f84c8b3dc9e393d03c09f3edc7841fe4c9c018d6ff7045f`.
- Artifact IDs / ZIP SHA-256: targeted `12021736431` / `92fff2edf68bd38f1dc952d7f5efaec78825337a2e5dd92aba07fa1506b6480c`; core `12021811183` / `f74cb48befc2c3b8b1c3960a7bbcc339a37364161d93deb97a379504aac5e74c`; APK/history `12021910160` / `f39a41ec02674b6a30c7d9ce5cac990c34da5175c8b5352a3f33be10f36b5432`; Android/visual `12022035973` / `684530423b222458acba31d1f93252ccc9aa01feb0fd324add0218ba5a39f80e`.

The complete contact sheet and full-resolution UIMSD home/sort-key images were reviewed clean. This is emulator/CI evidence only. The current APK certificate and application ID were not independently rechecked in this environment; no physical-phone, publication or tester-distribution claim is made. TEST231 remains the latest privately delivered fallback.

