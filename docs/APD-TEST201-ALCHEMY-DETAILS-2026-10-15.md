# TEST201 — visible alchemy craft details

Timestamp: 2026-10-15T09:14:47Z

Status: candidate pending hosted verification.

## Reproduced gap
The existing successful-brew path already sends ingredient-to-product lineage to the INTERNAL item recorder, but no PUBLIC/queryable observation identifies the craft the player just completed, its visible inputs/output, output quantity, floor, or energy split.

## Bounded change
- Add public telemetry 1.14 fact `alchemy.craft` only after `Recipe.brew` succeeds.
- Use the existing pre-brew ingredient duplicates so quantities and displayed names survive recipe mutation.
- Record displayed ingredient/result names, result quantity, floor, total energy, main-pool energy, and Alchemist's Toolkit energy.
- Keep true unidentified classes in existing INTERNAL item history. Failed brews and restore-time reconstruction record nothing.
- Preserve the existing `ApdItems.onCrafted` lineage call unchanged.

## Retained verification
Two new core methods cover the successful public payload/privacy boundary and failed/restore-time omission. `PlayWiringTest`, the fact manifest, telemetry version contract and permanent records history pin the integration.

Requested gate: 1,615 core tests, debug APK assembly, immediate-previous compiled TEST200/Codex TEST 196 history, 27 API34 methods and 35 valid 320×640 screenshots.

No recipe, inventory, energy, RNG, gameplay, save, score, custom-rule, application-ID, signing, delivery or physical-phone behavior is changed.

## TEST201 hosted result — failed contract
Source `776910414b7574b42cffa0d61e9bb7d4f1e83ca7`, run `38711834653`, executed all 1,615 core tests: 1,614 passed and `ContractTests.recordsHistoryDescribesTheCurrentTelemetryVersion` failed. The new milestone was placed before the required `APD records history, format 2` header.

History preservation/validation passed. APK assembly and API34 were skipped; no TEST201 APK exists. Failed core artifact `11960867915`, 752,666 bytes, SHA-256 `e040a5f4330624fd74a0739cf914eb553d34957dcda5d8464acb39f9e6f8befe`.

TEST202 moves only that milestone below the immutable format preamble; production source and all assertions remain unchanged.

## TEST202 hosted verification — 2026-10-15T09:36:13Z
Corrected source `186f5892429ef564ec4a9d59c9885a62fb6f1c1f`, run [38712411656](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38712411656) (09:22:07Z–09:36:13Z), passed the complete gate.

- Core artifact `11960394313`, ZIP SHA-256 `639308a8a783b67fc69319092ce802272be2d1ba8a0b8e207cd07796f6e5d1f0`: 1,615 tests in 315 XML suites, zero failures/errors/skips.
- APK artifact `11960578879`, ZIP SHA-256 `f1498ed29f368055d4910a63ba67d6dff77321bdbb6de67387384083ff1d3170`: `ADHD-Pixel-Dungeon-CODEX-TEST-198.apk`, SHA-256 `6dd8a5e16e13670e6dfb6abd12783dd7225ef8ed936b12b57f1192b51618936d`.
- Android artifact `11962595029`, ZIP SHA-256 `ebe2d31192960ccc23d2f2363da1932a0d5255070cf8173556314a2d2e782b5b`: 27 API34 methods, zero failures/errors/skips, and 35 valid 320×640 PNGs.
- Full 35-image contact sheet reviewed clean: no blank, corrupt or visibly clipped capture.
- Complete history matched immediate-previous compiled TEST200/Codex TEST 196, including the evidence-only files committed after that APK.
- Private delivery remained skipped. This is emulator/CI evidence only; TEST123 remains the last owner-delivered/signature-compared APK.

Telemetry 1.14 alchemy craft details are verified. No application build is pending; do not rebuild unchanged source.
