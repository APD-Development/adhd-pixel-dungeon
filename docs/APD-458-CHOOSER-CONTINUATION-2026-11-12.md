# APD458 chooser continuation, November 12, 2026

The last actually published and verified APK at the start of this effort was APD457, GitHub release `apd-457`. Preserve it, all older releases, the original signing certificate/application identity, saved data, and existing balance/score decisions. APD458 below is a new candidate and **must not be called a release until publication and asset readback succeed**.

## Verified source work

- Existing approved adaptive Search chooser allocation change: `04617f432a5fb0078bcc71f9656753d709691269`.
- New regression `0de7c2fb134505f18d059e3055695e24253bb3ba` in `SearchControlContextTest.java` verifies each returned chooser array is independent, persisted wall-search options remain in their original order, clearing memory removes unavailable wall choices, and a saved movement action stays preferred without wall-search memory. Its assertions also protect the no-hero case and non-action behavior.
- Exact source verification snapshot: `8c74997210585191d12603ad5e2ee398b1c54fe7` (includes the new test and its verification workflow).
- CI `43231740279` succeeded. Full core tests, desktop classes, Android debug/release and Android instrumentation Java compilation passed, with explicit new-case and zero-failure gates. CI `43231645349` independently succeeded after the regression commit.
- The only production edit after the previously published APD457 source is reuse of the chooser's fixed Action[] ordering, still returning a new caller-owned array, with no gameplay change. No score, RNG, saved fields, action costs, talent radius, or preferences changed.

## Frozen candidate pipeline

- Signed candidate builder workflow `.github/workflows/apd-candidate458-build.yml`, committed `8a2bb9c0c9c5a61795e4e84e1924741dcaf44b58`, started run [43231889687](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43231889687). It refuses production changes after source snapshot, checks the exact successful CI run, detaches to source snapshot, and builds Developer C458, Alpha-Testers P458 and instrumentation APKs with existing signing identity and versionCode 920. Its output is a candidate, not yet accepted.
- Native test helper `tools/apd458_native_postcheck.py`, commit `651ff5a02f46e8266f33f5dd267fbc9127e969de`, checks exact frozen source identity, one passing instrumentation case, and device-image SHA-256 transfer.
- Native workflow `.github/workflows/apd-candidate458-native.yml`, commit `f5f27f22128d3dbb3e419d8341b9fab04f7d4fcc`, run [43232032203](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43232032203). It must consume the **same original APKs**, never rebuild them, and complete WallGeometryDeviceTest, SearchContextDeviceTest and SeedHistoryDeviceTest for both variants, six real emulator cases.
- Gated publisher `.github/workflows/apd-publish458.yml`, commit `1eb5872be3f3dae33d6376f939db559412d79565`, run [43232110277](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43232110277). It must require source, candidate and all six native jobs successful with exact run/source bindings; verify APK hashes and source archive, publish `apd-458` as a prerelease, then download and hash-check the exact three assets. It refuses to overwrite an existing release.

## Next action

Read the actual terminal result of all three pending workflows. Never claim the six native tests or new APK delivery merely because jobs started. Inspect failed job logs and correct **only** the demonstrated blocker while preserving the original failure and frozen candidate. If all gates pass, verify `apd-458` release and direct Developer/Alpha download URLs and provide them immediately. Keep APD457 as the published fallback until that happens. Avoid concurrent branch overwrites or duplicate APD458 builds from the staggered coding tasks. Physical RedMagic install-over/audio checks remain unverified unless separately performed. Imported-seed and numerical scoring changes must follow the owner's already approved decisions, not invented formulas.
