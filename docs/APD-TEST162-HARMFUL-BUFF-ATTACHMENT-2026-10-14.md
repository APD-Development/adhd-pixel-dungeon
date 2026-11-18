# TEST162 harmful-buff attachment provenance candidate

Prepared 2026-10-14T17:53:07.187Z from actual branch head `7fd4e51d4ce6137d9c1baf84f39ac3dceb604fa4`, after verified TEST161.

The deferred pre-attach boundary is implemented without changing buff source objects, mitigation, turn costs or RNG. Newly created offensive Burning, Chill and Frost effects now carry immutable creator metadata while `Char.add(Buff)` is executing. Vault boss elemental form reactions scale that one immediate damage event exactly once, then discard transient attachment metadata. Burning fields prepare their saved single or mixed timed cause before attachment. Freezing fields now retain per-cell mixed ownership through decay and save/reload; old or environmental fields remain unattributed.

Updated known player/enemy/boss callers include fire/frost wands, imbues, armor and duelist abilities, cursed-wand effects, elemental and Yog effects, frost bombs, Frost potions, weapon enchantments and darts. Trap and room hazards remain environmental. No actor reference is serialized or retained. No historical missing origin is assigned a modern default.

Five retained regression methods cover player Burning, boss Chill, mixed saved Freezing fields, legacy environmental Freezing and neutral amount/source/RNG identity. Existing source-contract tests were updated to the pre-attach entry points. Static source audit found zero remaining production `Buff.affect(... Burning).reigniteAttributed...` attachment-after-attribution patterns among the inspected current sources.

No local Gradle repository workspace exists in this execution environment. Required exact-source verification is therefore pending: 1577 core tests, debug assembly, compiled history against immediate previous TEST161, 26 API34 checks and 34 valid screenshots. Three Damage controls remain disabled. This is a private candidate only; no delivery, release or physical-phone claim.


## TEST162 pre-build failure — 2026-10-14T17:56:56.152Z
- Exact source `50e61eb7d517d70566d8a06878a8709d1fd32e65`, run `38610738155` (2026-10-14T17:55:12Z–17:56:02Z) failed in history validation before compilation.
- Demonstrated cause: the comparison from compiled TEST161 source also contains its later verification record `docs/APD-TEST161-ARTIFACT-DERIVED-DAMAGE-2026-10-14.md`; that reviewed path was absent from the TEST162 manifest.
- Core, debug and API34 checks did not run; no APK or screenshots were produced. Application source was not tested and remains candidate-only.
- The manifest now includes that exact already-reviewed evidence path. TEST163 requests the same application source with corrected complete history. TEST161 remains latest verified; three Damage controls remain disabled.


## TEST163 compile failure — 2026-10-14T18:05:22.000Z
- Exact source `620562d4ce3e809aa90d2636ffc16ef3b0d0232d`, run `38611095532` (2026-10-14T17:57:44Z–18:04:15Z) passed complete-history validation, then failed `:core:compileJava`.
- Both errors were in `Buff.appendAttributed`: Java rejected private `apdPrepareAttach` calls through generic receiver type `T extends Buff`.
- The correction casts the receiver to the declaring `Buff` type before the same two private calls. No behavior, state, RNG, source or save-format logic changed.
- Tests, debug assembly and API34 did not run; no APK or screenshots were produced. TEST161 remains latest verified; three Damage controls remain disabled.


## TEST164 verification — 2026-10-14T18:23:48Z
- Exact application source `166c8a287b599fcb0f741a446f039fc33f656201`; GitHub Actions run `38612344678` ran 2026-10-14T18:06:32Z–18:18:07Z and concluded success.
- Core artifact `11917672775` (ZIP SHA-256 `a92f05678a40a517822e8cf3aee8a9e168dffb90078e97f31478b51526cf58b6`) contains 1,577 tests, 0 failures, 0 errors and 0 skipped.
- Debug APK assembled and complete reviewed history matched the immediate previous compiled TEST161 APK. APK artifact `11917912521` (ZIP SHA-256 `b1acddb6d3c46e1e3a1ba5e0b3b9595564402dffed2175359297f2e268ad4f12`) contains `ADHD-Pixel-Dungeon-CODEX-TEST-164.apk`, SHA-256 `5b6f46bebbb54f189142f4e0b1a14dbf7d204bf2a06f5fbae030efac3bee2919`.
- Android artifact `11918874056` (ZIP SHA-256 `99b8ade78132ca383f332901acc71c9180174120cc4ab641105f075f48b8492b`) contains 26 API34 emulator tests, 0 failures, 0 errors and 0 skipped. All 34 PNGs decoded as 320×640; the contact-sheet inspection found no blank, corrupt or visibly clipped capture.
- Verified behavior: harmful Burning/Chill/Frost attachment carries the actual single or mixed creator into immediate reactions and saved Freezing fields; environmental and legacy origin remain unscaled; neutral values preserve source, amount and RNG. No save-format change was introduced.
- Private delivery remained skipped. This is emulator/CI evidence only, not a physical-phone claim. TEST123 remains the last owner-delivered/signature-compared APK. Three Damage controls remain disabled pending six-class, boss-phase and rule-combination acceptance.
