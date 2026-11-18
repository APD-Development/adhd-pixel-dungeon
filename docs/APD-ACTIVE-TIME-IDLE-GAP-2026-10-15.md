# Active-play time idle-gap correction — October 15, 2026

## Reproduced boundary

`PlayTracker` says app-background and long-pause gaps are not play time, but its accumulator used `min(gap, 30 seconds)`. Every gap longer than 30 seconds therefore added exactly 30 seconds to `time.active` when the next hero turn began.

The retained regression moves the private monotonic baseline 31 seconds into the past, records the next hero turn and requires the active-time total to remain zero. Exact verified TEST232 source `96772420c7ccdcbc294e8eae33d68b27d71abfaa` must fail that method with 30,000 milliseconds.

## Bounded correction

- Count positive gaps up to and including 30 seconds.
- Count gaps over 30 seconds as idle and add nothing.
- Preserve the monotonic clock, running-total format and existing short-gap behavior.
- Do not change game time, turns, saves, raw history, scoring, gameplay, RNG, any Custom Dungeon Rule, or any already-recorded event.

## Acceptance boundary

The dedicated workflow must reproduce exactly one intended failure on TEST232 and pass all 1,695 current core methods after correction. The normal TEST233 gate must then pass debug assembly, complete immediate-previous produced TEST232 history, 33 API34 emulator methods and 42 valid screenshots. No automatic publication, tester distribution or physical-phone claim is authorized by this batch.

## Actual verification result

The acceptance boundary passed.

- Targeted run: [38847041242](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38847041242), 2026-10-16T03:44:46Z–03:47:59Z. Exact TEST232 source reproduced exactly one intended failure: the 31-second idle gap was credited as 30,000 milliseconds. Corrected source passed.
- Full run: [38847041245](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38847041245), 2026-10-16T03:44:46Z–03:59:24Z. It passed 1,695 unique core methods across 334 suites, debug assembly, complete immediate-previous produced TEST232 history, 33 API34 emulator methods and 42 valid 320×640 screenshots with 0 failures/errors/skips.
- APK: `ADHD-Pixel-Dungeon-CODEX-TEST-233.apk`, 38,586,337 bytes, SHA-256 `f3db3e79262ae45de213a34681df270f0b9ef662bb53eef1d84c2dc8e5f477af`.
- Artifact IDs / ZIP SHA-256: targeted `12024096254` / `267e93ae3d6d015f825871cc664c6cecb68c9d1f68ac4ae1aa82a01932568c1d`; core `12023821414` / `a9937a457cbc83b1ccdbf588eb0ac09cef52a18576125152ef5defc04480486b`; APK/history `12023681745` / `5c3b319a0efec8d5be9145cf80de2c93de5b908da7c9de97fb1e1c6043606626`; Android/visual `12024197452` / `ebe142a6f183383b6ce2b832c9a7b6a91b7e8c5b67e6dbb8e411fe40a9b7f317`.

The complete contact sheet and full-resolution UIMSD home/Custom Dungeon Rules images were reviewed clean. This is emulator/CI evidence only. The current APK certificate and application ID were not independently rechecked in this environment; no physical-phone, publication or tester-distribution claim is made. TEST231 remains the latest privately delivered fallback.

