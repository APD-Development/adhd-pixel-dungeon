# TEST165 boss-phase damage acceptance candidate — 2026-10-14

## Scope
- Started from verified TEST164 application source `166c8a287b599fcb0f741a446f039fc33f656201`, run `38612344678`, with actual branch head `7a4ff110e04f0005d5afe06bad9a67486d13c81d`.
- Audited every direct `.damage(...)` site in Goo, Tengu, DM-300, Dwarf King and Yog-Dzewa before changing source.
- Goo already delegates offensive rolls through the normal attack pipeline and needed no source change.

## Implemented
- Tengu bomb damage now applies the captured boss factor before its existing armor roll; the original `Bomb.class` source, roll, score effects and timing remain unchanged.
- Tengu fire and shocker fields now retain per-cell single or mixed creator provenance through decay and save/reload. Burning attachment and electric damage use that provenance; historical fields without metadata remain unknown and unscaled.
- DM-300 delayed falling rocks, Dwarf King summon-site collision damage and Yog-Dzewa death rays now apply their actual boss creator exactly once while retaining their original source objects and RNG calls.
- Dwarf King self-damage from `KingDamager` remains unscaled. No trap, starvation, environmental or Chalice behavior changed.
- Added retained acceptance covering all six current hero classes, Goo/Tengu/DM-300/Dwarf King/Yog classification, Tengu field save/legacy behavior, direct source boundaries, mitigation order and preserved self-damage.

## Verification state
- Expected complete core total: 1,581 tests (four new tests). Exact-source core, debug APK, immediate-previous TEST164 compiled history, 26 API34 checks and 34 screenshots are pending GitHub verification.
- Three Damage controls remain disabled pending successful evidence and the remaining high-HP/defense, save and cross-rule acceptance work.
- Private candidate only: no delivery, release or physical-phone claim. TEST164 remains the latest verified application source and TEST123 remains the owner-delivered/signature-compared fallback.


## TEST165 result — 2026-10-14T18:51:04Z
- Source `314b678c1aa7c85d4a84f6d5a02ad1929f43ecab`, run `38618183149`, compiled and ran all 1,581 core tests; 2 failed.
- Failure 1 was a headless-only particle-emitter null in the new Tengu fire test. Failure 2 was a real compatibility regression found by the existing Tengu shocker acceptance: new context-free Tengu fields lost their prior boss ownership.
- No debug APK, API34 suite or screenshots ran and no APK was produced. TEST166 carries the demonstrated correction; this report does not claim TEST165 verified.


### TEST165 retained failure evidence
- Failed core artifact `11920224194` was produced with workflow-reported ZIP SHA-256 `9d2031a309ed6aa6c5e4f69ba5b7afcbfe4252579acd9a81625484c3c6a5661f`.
