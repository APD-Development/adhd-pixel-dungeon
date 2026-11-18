# APD post-460 continuation: contextual Foresight caching, November 12, 2026

## Actual source work and verification

This handoff records new, already merged source work, **not** a newly assembled APK. APD460 remains the most recently confirmed accepted and published Developer/Alpha-Testers release while separately developed APD461 is still going through its own native/release gates. Always recheck live releases and branch heads.

- Original base: accepted APD460 development head `b020d5d511549d969e8c593a43f31d7edb24e90f`.
- Isolated development branch: `codex/apd-foresight-context-cache-20261112`; the overlapping APD461 malformed-observed-terrain work remained isolated on another branch and was not altered.
- `1f40950af2c635449b29a9a15a3c8c26fb7030f4`: introduce a package-private `WallSearchSession.covers(..., boolean foresight)` path that retains all existing position, geometry, FOV, and normal/foresight semantics. Public `covers(...)` remains available and observes its own live buff as previously.
- `960863dbf7b6aa229acaf6ee9e888f4f076f761c`: in read-only `SearchControlContext.hasMeaningfulWall`, observe Foresight once before scanning and pass that value to all candidate coverage checks instead of re-querying the buff for each tile.
- `fbbdd8ff20630bde39c234fda92abdc9241973fb`: add `cachedForesightScanMatchesOrdinaryCoverageForEveryClassAndAcquiredRank`. The real Hero test compares every tile on a 21x21 grid for every HeroClass and existing Wide Search rank, then active Foresight across visible and non-visible tiles. It also checks that no game action or turn is performed.
- `f885b0a69b5f77432efee7f6219d8fbecea89a6a`: source CI workflow `.github/workflows/apd-foresight-context-ci-20261112.yml`, requiring the exact cache wiring and new regression, complete core suite, and desktop / Android debug, release and instrumentation Java compilation.
- **GitHub CI run 43241975098 completed successfully:** raw log verifies **3,267 tests across 470 suites, zero failures/errors/skips**; the required compile targets and new regression passed. The test-run artifact remains under the original workflow.
- PR #7 was merged into the existing development branch in merge commit `0ece96536ec3026eb709b90ca14404ac65c5abf4`: https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/7. The post-merge validation run `43242614205` was still in progress at this checkpoint; check its eventual terminal result separately.

## Guardrails and next continuation

This is a bounded performance optimization only: no Search radius, room geometry, wall eligibility, Foresight effects, talents, action costs, score formulas, RNG, saved history, saved game schema, APK signing identity or version code was intentionally changed. It reuses a value within one synchronous read-only scan and preserves the public dynamic `covers` method for other call sites.

Do not independently call this APD461 or APD462 or claim an APK exists: a new candidate must be assembled, signed, tested on Android, and published with fresh verified download hashes. Preserve accepted APD460 and earlier releases. In particular, the separate APD461 malformed-terrain fix and its frozen application/source/native pipeline must not be dropped, overwritten, or implicitly certified by this source-only result. Check whether APD461 completed and merged before preparing any subsequent candidate.

Physical RedMagic install-over, Prison/Tengu sound timing and the owner's unapproved design decisions remain separate. Scoring mechanics must not be altered on speculation.
