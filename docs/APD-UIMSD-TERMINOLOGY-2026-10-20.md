# UIMSD extensible terminology checkpoint — 2026-10-20

## Result

Core-verified source: `16b1686a7640ab3d025a29ec770ae88043c94405` (parent `0485222428916b5c7ebd7241f4c469c0df51f8b7`).

The UIMSD view search, schema/topic discovery and exact offline question interpreter now share one immutable terminology registry. A future dataset or public column can add translated aliases without changing either parser. Measures and the small grammar's connector words are registered through the same object. This remains an exact schema interpreter, not arbitrary natural-language inference.

## Boundaries retained

- Aliases describe registered schema only; recorded row values are never indexed.
- Internal/hidden columns are excluded even if an alias is registered for them.
- Ambiguous dataset or column aliases return no interpreted question.
- Unsupported conditions are not stripped or guessed.
- Vocabulary extensions create a new registry and cannot mutate another active/default session.
- Existing queries, persisted questions, raw histories and gameplay recording are unchanged.
- Everything remains offline; no paid or external language service was introduced.

## Changed source

| Path | Purpose |
| --- | --- |
| `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/UimsdTerminology.java` | Immutable dataset/column/measure/connector vocabulary and built-in aliases |
| `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/UimsdSearch.java` | View search consumes the shared vocabulary and retains public-schema-only indexing |
| `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/UimsdTopicSearch.java` | Topic discovery and exact interpretation consume the same vocabulary |
| `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/query/UimsdTerminologyTest.java` | Five extension, isolation, hidden-field and fail-closed regressions |
| `docs/APD-CODEX-CORE-TEST-REQUEST.txt` | Exact complete-suite request; no APK request |

## Verification

GitHub Actions run `39437010997` completed successfully for the exact checkpoint. Artifact `12272916909` is 953,258 bytes with digest `sha256:59c9304316030081b08a8f954e968b7716bf463927ce9c5b8c1f9b011009ee65`.

The extracted JUnit XML reports:

- 2,013 tests
- 392 suites
- 0 failures
- 0 errors
- 0 skipped

All 2,008 previous methods, including the final mimic/electricity tests, remained in the run.

## Completion state

Completed at core level: extensible offline terminology shared across the three discovery/interpretation paths, with exact Spanish-alias coverage and safety boundaries.

Still pending: native/API34 acceptance of the new search/question paths; general correlated windows beyond the fixed equipment-to-damage window; timeline/death completion; broader temporal experiments; comparable-condition progress; representative Android storage/latency profiling.

No APK was built or named by this checkpoint. TEST303 remains the newest produced paired APK, and the newest actually published owner APK remains Alpha Preview TEST262 from 2026-10-16. The build request was not edited.
