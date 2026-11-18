# TEST126 preparation — 2026-10-13

Poison duration contributions retain player/enemy/boss/unattributed causes through extensions, stronger reapplications, swarm splitting and save/load. Each raw contribution receives its matching factor once; boss replaces enemy. Weaker poison does not steal ownership. Neutral math and RNG remain unchanged. Legacy absent metadata remains unattributed; malformed optional metadata is rejected without modifying raw saves. Cursed-wand callbacks retain cast-time origin with exception-safe thread-local lexical scopes.

12 new permanent tests passed locally in 0.253s after the RNG forecast assertion; all 13 modified production files compile locally. Tests cover real poison ticks, all permitted damage percentages, mixed-source rounding, metadata validation, save/load, zero-duration behavior, forecast accuracy and no RNG consumption, nested scopes and cross-thread isolation. They do not establish complete offensive-source coverage.

Full 1408 core / 24 API34 / 27 screenshots / debug / compiled-history checks pending. Player/Enemy/Boss Damage controls remain disabled: burning, corrosion, bleeding, blobs, other spells, explosions, derived/shared damage and other attributable sources still require integration. No scoring changes or range changes. Mandatory permanent custom-rules UIMSD acceptance remains outstanding. No physical-phone verification. TEST123 remains delivered signed-compatible fallback.

Previous actual APK is TEST125 source8384225f38df7313183cd583e12a6b733df6aed7/run38442590584, verified1396core+24Android/27PNG. Every new source/test/document path has an individual or shared verification changelog entry. No private tester distribution requested automatically. Preserve app ID, signing identity and all player data.

## Verification failure and retry
Run38451155781/source1f779aa26165fea757e7deb43cf9f618a93b6b22 failed offline-history validation: four enemy entries accidentally shared identical generic descriptions, rejected by the duplicate-description guard. No core/build/Android success or APK resulted. Corrected each entry with a specific enemy description without weakening the guard; TEST127 retry retains the same application changes and compares against actual TEST125 APK. Local12checks remain passed; fullchecks pending.
