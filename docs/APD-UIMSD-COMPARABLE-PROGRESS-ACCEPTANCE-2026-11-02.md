# UIMSD Comparable Personal Progress — native API34 acceptance

Timestamp: 2026-11-02T20:29:14Z

## Disposition

The production Comparable personal progress flow has native API34 behavioral acceptance on the unchanged exact TEST345 developer APK. The new method passed twice. The first complete-suite attempt retained one unrelated warm-Home timing failure; the unchanged retry passed all 68 methods. New-screen visual artifact acceptance is still pending because the screenshot filename was not in the shared pull allowlist.

This does not create or rename an APK, publish or distribute anything, or claim physical-device evidence. Current branch application inputs advanced afterward through the separately authorized QoL package, so this exact-APK result must not be relabelled as evidence for that newer application source.

## Exact sources and artifact provenance

- Test source: `e7fce780ff2ff67fa6e53a9ad96dcc65397cc60e` (merged through `5362acb17a4dc34cc36c48e450564a44c04822b4`).
- Exact application source: `207ed6819ed2a36cd4f6ccd02eaca5d7b9aa1457`.
- Producer run/artifact: `41344574181` / `13097999436`.
- Developer APK SHA256: `a6c5653d5eaf182c65a5352146ce8660cdfd0f71ad70796049a87dc35b3e5b46`.
- Provenance receipt: `application_rebuilt=false`, `application_inputs_unchanged=true`, `physical_device=false`, `published=false`.

## Accepted native path

The pointer-driven method `comparableProgressCapturesExactCohortsAndOpensContributors` uses 60 finished synthetic rows:

- 30 earlier rows have exact start times 1000–1029 and recorded score 10.
- 30 recent rows have exact start times 2000–2029 and recorded score 30.
- Every row has the same exact Mage class, APD version, difficulty 3, balance 0, challenges 0 and Custom Rules identity.
- The UI captures the current earlier query as cohort A and the current recent query as cohort B.
- Score displays `20.00` as recent minus earlier.
- The recent query and original row scores remain unchanged after native window restoration.
- `Recent exact metric contributors` contains 30 rows; the first selected row is `run-30`; Back navigation remains available.

## Complete-suite evidence

### Attempt 1 — retained failure

- Run/job: `41515078067` attempt 1 / `125516460785`.
- 68 methods executed; the new comparable-progress method passed.
- Sole failure: `warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms` measured opening 1 at 256 ms, 6 ms over the unchanged 250 ms guard.
- Artifact: `13176610306`, 7,221,209 bytes, digest `sha256:c6e076e685f8c08f20834eab9a5e2068c9f295633b963fac912524e6e85502ff`.

### Attempt 2 — accepted retry

- Run/job: `41515078067` attempt 2 / `125524145734`.
- Result: `OK (68 tests)`, 0 failures, elapsed 546.978 s.
- Artifact: `13177051685`, 7,390,012 bytes, digest `sha256:49d60787f6776bba2244e10259f1d02cbb1bd38589de18cfc9a9e9864454fd18`.
- The artifact ZIP digest was independently reproduced after download.

## Visual evidence limitation

Both artifacts contain the prior 92 validated 320×640 PNGs. The passing test called the standard screenshot helper for `uimsd-comparable-progress.png`, but `tools/run-codex-android-checks.sh` only pulled its explicit historical allowlist, which omitted that new filename. The screenshot therefore remained on the disposable emulator and is not accepted as archived visual evidence.

The allowlist now includes the new filename for the next applicable current-source native/display run. No redundant exact-TEST345 run is launched because the approved branch now includes separate QoL application changes; the old input-equality assertion would not describe that advanced application tree.

## Preserved boundaries and next step

TEST345 remains the latest private emulator-verified candidate. TEST341 release `428527455` remains the latest actual private publication; TEST330 remains fallback. No physical-device, publication, distribution, build, signing, application-ID, save, statistics, scoring, Custom Rules, combat or RNG claim changed.

Next: run the merged current application source through its authorized native/display acceptance, including the corrected comparable-progress screenshot allowlist and the complete API34 suite. Keep the QoL source-verification evidence separate until that succeeds.
