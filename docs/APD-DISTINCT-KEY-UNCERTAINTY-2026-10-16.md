# APD distinct-key uncertainty correction — 2026-10-16

## Confirmed defect
A per-distinct aggregate used a private loose identity for each row whose distinct key was hidden, missing or otherwise unavailable. This deliberately prevented unknown keys from being merged or dropped, but the resulting `AggValue` was not marked `UNSURE`. Lower-bound keys were also accepted as exact identities. The table could therefore present an exact-looking count or rate even though the distinct identity was inexact.

## Bounded correction
- Keep every inexact-key row and preserve the existing numeric result.
- Use an exact shared key only when the distinct value is visible, known and not lower-bound.
- Mark the aggregate uncertain for every loose identity so the existing detail formatter discloses that some entries could not be judged.
- Leave exact known distinct keys and all aggregate arithmetic unchanged.

## Retained acceptance
`rowsWithNoValueToShareAreNotMergedByTheDistinctRule` still proves two missing keys stand for separate rows, and now requires uncertainty plus the UI disclosure. `lowerBoundDistinctKeysAreNotTreatedAsExactIdentities` proves a lower-bound key is not treated as an exact shared identity. Exact TEST239 source `6182b042b1d93b64341f41a516a49ef3d12c93ee` must fail both methods; corrected source must pass all 1,707 core methods.

## Required TEST240 gate
Dedicated before/after evidence, complete core suite, debug assembly, complete immediate-previous TEST239 APK history from run `38881765134`, 33 API34 emulator methods and 42 valid screenshots. No automatic publication, tester distribution, physical-phone result or owner-device install-over/save claim is authorized.

No raw histories, saved views, cache formats, saves, scores, gameplay, Custom Dungeon Rules, turns, RNG, Cleric lore/private-grudge/opt-out behavior, signing or application ID are changed.


## 2026-10-16T11:03:00Z — TEST240 distinct-key uncertainty fully verified
- Exact source `7f18ca8bcbabc07f159b6e3acfc9689c1ae6f28b` completed [full run 38889483083](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38889483083) successfully from 2026-10-16T10:46:55Z through 2026-10-16T11:00:18Z.
- Dedicated [run 38889483125](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38889483125) reproduced exactly two intended failures on unchanged TEST239 application source `6182b042b1d93b64341f41a516a49ef3d12c93ee`: `rowsWithNoValueToShareAreNotMergedByTheDistinctRule` and `lowerBoundDistinctKeysAreNotTreatedAsExactIdentities`. Corrected source passed all 1,707 unique core methods across 335 suites with 0 failures, errors or skips.
- Debug assembly and complete immediate-previous produced TEST239 history passed. Independent comparison found all 285 TEST239 history TSV records unchanged; TEST240 adds only `codex-test-240.tsv` plus the generated index update, for 286 records. The new record contains both reviewed categories.
- Independent artifact recount confirmed 33 API34 emulator methods with 0 failures, errors or skips and exactly 42 PNGs, all 320×640. The complete 42-image contact sheet and full-resolution UIMSD text-entry view were reviewed without a blank, malformed or visibly clipped capture; logcat contained no fatal app-exception, ANR, native-abort or fatal-signal signature.
- Produced APK: `ADHD-Pixel-Dungeon-CODEX-TEST-240.apk`, 38,589,474 bytes, SHA-256 `46666aaad0a2a21a859ba57156eb8789ba704b93df67775f5448797091c28f3a`. Compiled version `4.0.8-apd-INDEV-bC240-TEST`, application ID `com.charltonchris29.adhdpixeldungeon.indev`, and extracted v2 certificate SHA-256 `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e` match TEST239 identity.
- Artifact IDs / independently downloaded ZIP SHA-256: targeted `12040539281` / `a7008bdd5e4966eb464d3be4bdb5065b7ebedc294bbebd874c8944970c73a57c`; core `12041590231` / `29cec783f0c0be0c443cdc1f217b3523277b25455bfe51b2261d6f0ff9143002`; APK/history `12040799585` / `5f551fe1fb8f1f0453d5f0a9c2a1181839acc35a79ecb809accc036b072da7e5`; Android/visual `12042111015` / `d95ce848a1b8e72fb221acdcc7a41493063eca1b1d7a9400e8b95a74283152cb`.
- Scope remains only uncertainty disclosure for per-distinct aggregates with hidden, unknown or lower-bound identities. Their rows and prior numeric results are preserved. Raw histories, saved views, caches, saves, original scores, gameplay, all 18 rules, turns, RNG, all 50 Cleric lore entries/private grudge/opt-out, signing and application ID were not changed.
- TEST240 is a fully CI-verified candidate only. No publication, tester distribution, physical-phone result or owner-device install-over/save acceptance occurred. TEST231 remains the latest privately delivered fallback.
- Do not rebuild unchanged TEST240 solely to record evidence. Continue the broad UIMSD reliability audit; audio and unconfirmed armor/sentry acceptance remain open.
