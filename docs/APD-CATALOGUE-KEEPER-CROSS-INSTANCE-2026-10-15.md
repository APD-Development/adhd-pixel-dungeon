# Catalogue keeper cross-instance correction — October 15, 2026

## Reproduced cause
`CatalogueKeeper.offer` was synchronized only on one keeper instance. Two keepers targeting the same file used the same `.tmp` name and compared a candidate only with their own in-memory `held` value. A stale keeper created before another instance accepted sequence 2 therefore accepted and persisted signed sequence 1, undoing the newer catalogue and any revocations it carried.

## Bounded correction
The compare-and-replace section is serialized across keeper instances. Inside that section each offer re-reads and verifies the persisted catalogue, adopts it when newer than local state, and rejects equal or older sequences before writing. Atomic replacement, signature/purpose/time checks, expired-cache revocation protection and caller-visible results are retained.

This is local provider-neutral storage reliability only. It does not activate a backend, network access, accounts, content downloads or online play. It changes no raw run history, save, score, custom rule, gameplay, RNG, signing key or application ID.

## Retained regressions and gate
Two new `ContentCatalogueTest` methods cover the original stale replay and deterministic cross-instance replacement serialization. The original source accepts the stale sequence-1 replay after sequence 2; corrected source must retain sequence 2. A second keeper cannot enter the shared replacement section while the first is paused there, and the final file reloads as sequence 2 without a leftover temp file.

TEST215 requests 1,652 core tests, debug assembly, complete immediate-previous TEST214 history, 28 API34 methods and 38 valid 320x640 screenshots. Candidate pending until actual workflow evidence completes. TEST214 remains signing-compatible staged evidence; published TEST206 remains the owner-delivered fallback. No physical-phone claim.


## Verified TEST215 evidence
Exact source `0507127f47426405a72ca83d0eccf04e1ce04752` passed [workflow 38779254822](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38779254822), started 2026-10-15T17:40:14Z and completed 2026-10-15T17:53:26Z.

- Independently downloaded core artifact `11994614480`, 770,505 bytes, ZIP SHA-256 `0a73f868cd553535316f97ea06fc4ae4e5e3d24ba34a22601a68c678b13f01cf`: 1,652 tests in 326 XML suites, zero failures/errors/skips. `ContentCatalogueTest` ran 11 methods including both new keeper regressions.
- Debug assembly and complete immediate-previous TEST214 history validation passed.
- Independently downloaded Android artifact `11995607233`, 3,309,386 bytes, ZIP SHA-256 `57df629add2e16175db27e8ec0f2b7f24ff35d8f1f1a30b7f1b24a2ae70bf848`: 28 API34 methods, zero failures/errors/skips. All 38 PNGs decode at 320x640; the complete contact sheet was opened and reviewed readable.
- APK artifact `11994659353`, 36,062,230 bytes, ZIP SHA-256 `834462f16de2aea3b636288b937cf06f229faa7dd8c0791811cdb547ead59d85`. Contained `ADHD-Pixel-Dungeon-CODEX-TEST-215.apk`, 38,559,464 bytes, payload SHA-256 `0d0dced41e6b541f07b65bb6feb71fa17775d22327890c94cbff6deb77730bed`; the packaged checksum file matches.
- Private delivery stayed disabled. No signing comparison, publication, owner-save install-over or physical-phone claim. TEST214 remains the signing-compatible staged alpha candidate and published TEST206 remains the owner-delivered fallback.
