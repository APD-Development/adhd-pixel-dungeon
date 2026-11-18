# APD UIMSD query serialization acceptance — 2026-10-17

## Result

The current `QueryCodec.java` blob `3a643d13367ca9ef51cc109667fc7bf7b52ee368` was read completely with its Query, Filter, Aggregate and SavedViews consumers. No production defect was confirmed.

Test-only commit `c2014077526dfa87b6062ab1762548d74d632290` adds `queryCodecPreservesAllEscapedFieldsAndEmptyTextCardinality` to `QueryEngineTest`. The retained contract verifies:

- tabs, newlines, pipes and backslashes round-trip across dataset, ruleset, filter, sort, column, group and aggregate fields;
- one empty text value remains distinct from an empty text-value array;
- aggregate labels, `Long.MIN_VALUE` targets, `Long.MAX_VALUE` conditions and optional distinct/ratio fields survive;
- encode/decode/encode produces the same bytes.

## Full core evidence

- Request commit: `27de932775034c51ba7ea5c33063056204142d77`
- Workflow run: `39006675168`
- Started: `2026-10-17T05:33:08Z`
- Completed: `2026-10-17T05:35:33Z`
- Result: success
- JUnit XML: 350 suites, 1,771 tests, 0 failures, 0 errors, 0 skipped
- `QueryEngineTest`: 21 methods passed
- New method duration: 0.0 seconds
- Artifact: `12091417221`, 828,440 bytes
- Artifact digest and independently downloaded ZIP SHA-256: `4c94568a467fcfe8198024a5372ab6a0eb4e0269d7ffaa6067f23f3a412e6bc3`

## Change boundaries

This is test and documentation only. Production application source, `docs/APD-CODEX-BUILD-REQUEST.txt`, APKs, signing, application ID, saves, permanent statistics, raw history, all 18 Custom Dungeon Rules, v2.6R scoring, original scores, gameplay, turns and RNG are unchanged.

TEST264 remains the latest verified/staged private candidate. TEST262 remains the latest actually published owner APK. No physical-phone, publication, or tester-distribution claim is made.

The exact current-blob UIMSD review count is now 101. The broader audit and gameplay acceptance backlog remain open.
