UIMSD seed-floor allocation cleanup — locally verified, not yet committed or APK-verified.

Reviewed DatasetStore, RowCodec, RowMerger, Row, Dataset, DatasetDef, Catalog, SeedFloors, ReplayFloor, ResultTable, GroupDetail, CellText, ColumnOrder and Sort. This is a partial audit; it does not declare every UIMSD component reviewed.

Confirmed allocation: SeedFloors constructed a temporary Dataset and an empty RunInfo list for each output row solely to obtain DatasetDef.size(). The local change passes the existing DatasetDef directly; row contents and calculation paths are unchanged.

11 freshly compiled existing SeedFloorsTest cases passed in 2.919 seconds with libGDX native library and existing game assets. Initial setup attempts lacked the native/asset paths and did not verify the change; those were corrected before the passing run.

10,000-row deterministic canonical fixture, ThreadMXBean allocated-byte measurement after five warmups:
Original: 6,657,912 allocated bytes.
Changed: 6,177,912 allocated bytes.
Reduction: 480,000 bytes (48 bytes/output row) in this host/JVM fixture.
All row IDs, value types, provenance, numbers and text content yielded identical hash 8221444730120799617. Timing varied (original 16.9 ms, changed 27.7 ms); no runtime speedup is claimed. JIT/Android allocation results may differ.

Separately, current query/stats source was freshly compiled and 38 DatasetBuildersTest/RowMergerTest cases passed in 1.017 seconds. No raw logs, player saves, capacity, scoring or mechanics were changed.

Isolated 10,000 tiny synthetic logs: cold load 6,031 ms, 10,000 rebuilt; cache load 2,523 ms, 10,000 cached. Both retained 10,000 runs, 50,000 rows across 38 datasets under a 256 MiB heap. Not a test of large real histories or Android performance. Initial 1,000-run probe used precompiled dependencies and is not substituted for this fresh-source result.

Full Android verification and release inclusion are pending; do not call this delivered. TEST91 verification is independent of this local-only cleanup.

Additional freshly compiled UimsdModelTest, QueryEngineTest, SavedViewsTest, CellFormatterTest and TableGeometryTest: 64 cases passed in 1.909 seconds. First launch omitted the required apd.query.src property (63 passed, 1 fixture failure); corrected launch passed without source or assertion changes. Together with storage/merger (38) and seed-floor (11), 113 distinct existing cases passed across eight test classes. These local checks do not replace full APK or Android checks.
