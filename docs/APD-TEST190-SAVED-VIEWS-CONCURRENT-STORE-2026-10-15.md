# APD TEST190 — SavedViews cross-instance store serialization

Candidate prepared: 2026-10-15T02:40:14.718Z
Branch: `codex/apd-phone-fixes-2026-10-09`
Immediate previous compiled APK: TEST189, source `ea1062079c6a43aadaf80dbf0197f3fcff4769cc`, run `38669986563`

## Reproduced source defect

`SavedViews.store` used the fixed sibling path `saved_views.txt.tmp`, but its write and rename were not serialized across separate `SavedViews` objects. Two UI/store instances targeting the same history root could therefore open and truncate the same temporary file concurrently, then race their rename. Instance synchronization would not protect this shared on-disk resource.

## Bounded correction

A private static lock now covers only the small directory-create, temp-write and rename replacement section. A package-private test hook pauses the first writer after opening the temp file. The retained two-thread regression proves a second independent instance cannot open the shared temp path until the first finishes, then requires both calls to complete, the final second view to reload cleanly, and no temp file to remain.

The partial-load refusal remains outside mutation and unchanged. Saved-view format, query encoding, raw run logs, player saves, permanent statistics, gameplay, scoring, application ID and signing are unchanged.

## Requested verification

TEST190 must run the complete 1,604-core suite, debug assembly, immediate-previous compiled TEST189 history comparison, all 27 API34 methods and all 35 expected 320×640 screenshots. This candidate is not accepted, delivered or physical-phone verified until exact-source evidence passes. TEST189 remains the verified hosted/emulator fallback and TEST123 remains the last owner-delivered/signature-compared APK.


## Hosted result — native and build gates passed; binary content review pending

Run: [38672320647](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38672320647)
Source: `97c4848581e2f7fcd979d3965e257bdae59c5a93`
Run window: 2026-10-15T02:41:30Z–02:55:08Z

- The complete core job passed, including the new retained cross-instance store regression, debug APK assembly, and immediate-previous compiled TEST189 history validation.
- API34 finished all 27 configured methods with 0 skipped and 0 failed; the Android job concluded success.
- The runner validated and listed all 35 expected PNGs at 320×640.
- Artifacts: core `11944263722` / ZIP SHA-256 `7b8a3adfb3fffed6ff5a592830f2c839d19db22339347fdd3356f78d3526504d`; APK `11944698529` / ZIP SHA-256 `09e0246462b86d4d50793e09e126791d16c0f1aa08a1cef9f3f751de4d3a5f93`; Android `11945087238` / ZIP SHA-256 `5152ec875abf965a3e64176daeb2fd6fe48bd11ee08531747490de2925e67431`.

The connector exposed the artifact references and complete job logs, but this runtime could not open the private ZIP payload for an independent XML total, contained-APK checksum read, or contact-sheet content review. Therefore the exact expected 1,604 count is not relabeled as a parsed XML count, and TEST190 is not yet promoted above TEST189 as the fully reviewed checkpoint. Reuse these exact artifacts for that remaining audit; do not rebuild unchanged source.

No APK was delivered and no physical phone was used. TEST123 remains the owner-delivered/signature-compared fallback.
