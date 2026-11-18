# TEST191 archive compression-wrapper lifetime — 2026-10-15

Status: hosted/emulator verification complete; no delivery or physical-phone claim.

Current exporter/importer source created ZIP wrappers but never closed them, leaving inflater/deflater cleanup to garbage collection after callers separately closed Android document-provider streams. This controlled source finding is not an observed phone crash or data-loss claim.

The bounded correction closes exporter/importer-owned ZIP wrappers on every path through non-closing adapters, preserving caller ownership. Two tests cover both public caller streams and direct importer ZIP closure. Archive format/manifest, raw history, saves, permanent statistics, scoring, gameplay, application ID and signing are unchanged.

Verification run [38677900668](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38677900668) passed 1,606 core tests (310 XML) and 27 API34 tests, zero failures/errors/skips, debug assembly, immediate-previous compiled TEST190 history, and 35 valid 320x640 screenshots. The full contact sheet reviewed clean. APK SHA-256: `b88b4ddc71f12abaac8d216d3c553968577b254b9f55f3ecff2522ef0046329b`.

Artifacts: core `11946906120` / ZIP `48b92ba33cb0ec901db327f1f9de6099e4ff4e411c9d3b164cdfcd31ef3e66d9`; APK `11946966071` / ZIP `89c853dd6650c38cc75ebbef3eee3a7a32f58091a10f534de2232b9c967d5099`; Android `11947432170` / ZIP `e53779a6a8502d817d0fdb253bbcd303baa9dab8a6bd8ae8bdc0bdaa6a19c830`. No delivery or physical-phone claim.
