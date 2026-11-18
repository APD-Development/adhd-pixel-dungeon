# UIMSD warm-cache content identity verification — 2026-11-05

## Result

A bounded stale-statistics defect is fixed and source-verified. The title preload can no longer treat different raw UIMSD inputs as unchanged merely because their file size and modification time match.

Exact production commit: `131e6551a62f660f16de7e71439856872f622ca5` (`2026-11-05T20:08:32Z`).

## Reproduction

The permanent regression creates two valid telemetry logs with different scores but identical byte length. It prewarms score 31, replaces the raw file with the score-47 bytes, restores the original modification time exactly, then requires the prepared catalog to be rejected and released before a fresh load observes 47.

The first attempted red commit `4681c6366fee25a76a2726d28eedf39aa24dffdc` (`2026-11-05T20:00:55Z`) contained a test-fixture package error. Run [42099359414](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/42099359414), job `127497758893`, ran from `2026-11-05T20:01:00Z` to `20:02:50Z` and failed `:core:compileTestJava` before any test. Its 289-byte artifact `13449136460` has `sha256:c901a82bff4659c7d836ab901eaa1b19a822e53f37eaedb8b9af192b5ef1c959`. This failure is retained and is not used as proof of the production defect.

Fixture-only commit `48b9160bd39eb547f5a984b4905c0b2c10e95b7b` (`2026-11-05T20:04:00Z`) corrected the package reference without changing production. Controlled red run [42099850878](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/42099850878), job `127499469017`, ran from `2026-11-05T20:04:03Z` to `20:07:19Z`:

- 2,499 tests in 441 suites;
- exactly one failure: `UimsdWarmCacheTest.sameLengthAndTimestampRawReplacementInvalidatesPreparedCatalog`;
- 0 errors and 0 skips;
- no APK assembled and no physical-phone test;
- artifact `13449431835`, 479,446 bytes, `sha256:8adc9480f183491333182cdcee74f7efc3f3c41ef5bedbcfa5c69514e47a0c33`.

## Correction

`UimsdWarmCache` now includes SHA-256 content identity in its stamp for:

- every eligible `runs/*.apdlog` and `legacy/*.apdlog`;
- the saved column-layout file;
- the demo marker file.

Existing sorted path and metadata identity remains. A read or digest error returns no stamp, so uncertain input is never accepted as fresh. The 64 KiB streaming buffer and all reads execute on the existing optional prewarm or interactive open worker; no render-thread hashing was introduced. Existing memory admission still limits optional title preloads, and rejected catalogs retain deterministic release before a cold replacement.

## Green verification

Run [42100583574](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/42100583574), job `127501985535`, started `2026-11-05T20:08:36Z` and completed successfully `2026-11-05T20:11:46Z`:

- 2,499 tests / 441 suites;
- 0 failures, 0 errors, 0 skips;
- Android debug Java, release Java and instrumentation-test Java compiled;
- desktop classes compiled;
- build successful in 2m41s;
- no APK assembled; no emulator/API34 runtime, visual, tester or physical-phone claim;
- artifact `13449498676`, 479,711 bytes, `sha256:7afe8eb2248a5fce2b1e046d80c0723b57e9b1de6078363fced12b48665bdee1`, expiring `2026-11-19T20:11:42Z`.

## Preservation and release status

TEST364 remains the latest actual private release: application source `1103a2e7caf72a094836a7cf6931468f3f800b36`, tag `apd-alpha-preview-364`, release `435592879`, published `2026-11-05T18:47:23Z`. This later source fix is not in the TEST364 APKs; no new APK name or delivery is claimed. TEST362 remains a preserved fallback.

The executable build request was left byte-for-byte untouched at blob `dc2905f5e054fe8eff2953ccf440eefc6725fdc7`. No raw history was rewritten or deleted. Saves, permanent statistics, original and v2.6R scoring including all 357 lookup values, all18 Custom Rules, gameplay/RNG, application ID, signing key and existing release assets are unchanged.

## Remaining acceptance

The next deliberate application candidate containing this fix must complete the full native API34 inventory, six display profiles, actual Alpha-Testers acceptance and immediate-previous compiled-APK history comparison. Physical install-over acceptance, the unconfirmed TEST254 fire report and broader representative-device UIMSD latency investigation remain open.
