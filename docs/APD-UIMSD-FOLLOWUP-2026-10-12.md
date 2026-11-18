# UIMSD follow-up October12,2026

Baseline: exact TEST73 application source2a94b96a35be65ae4792b123dc2144ec1c7ca881. Delivered TEST73 remains the playable rollback.

Reviewed full QueryEngine, DatasetStore and AggValue source and the existing aggregate, dataset-builder, per-unit and import tests. QueryEngine created both an unused HashSet and ArrayList for each ordinary aggregate. Create those only for COUNT_DISTINCT and MEDIAN respectively. No calculation, provenance, filter, record or persistence changes.

Controlled desktop allocation probe:20,000 SUM calls on1,000rows after10,000warmup calls, JVM escape analysis disabled to measure actual allocations. Baseline4,000,160bytes; corrected2,240,160bytes (1,760,000bytes fewer,88bytes/call). Timing82.3ms vs87.0ms is noisy and does not establish speedup; no Android performance claim.

Exact-source QueryEngine tests20 passed; combined query/aggregate/default views/per-unit/dataset-builder checks90 passed0.650seconds with corrected QueryEngine. An initial fixture incorrectly searched merged row IDs by run ID and failed; corrected fixture uses Row.runId. Actual reopened-log/cache test passes unchanged DatasetStore, so suspected duplicate hiding was NOT reproduced and DatasetStore was NOT changed. Initial local combined compilation omitted SyntheticLog helper; corrected classpath resolved it. These were local test setup failures, not APD bugs.

New permanent resumed-run/import/cache regression is retained. Full current-source core/debug/API34 checks requested, not yet passed. Entire UIMSD audit/redesign and physical-phone testing remain incomplete. Owner added0.5x/1x/2x game-speed priority with4PMEDT target; playback-vs-mechanics question pending, independent work continues.
