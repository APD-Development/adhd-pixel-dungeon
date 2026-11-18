# APD typed-payload statistics provenance correction — October 15, 2026

## Confirmed defect
A valid log frame can contain a payload its event decoder cannot read. TEST226 RunFactsBuilder already treats surviving run counts as incomplete, but RowBuilders checked only framing errors, GAP markers and mid-run starts. Item and enemy totals from that same raw history could therefore appear exact.

The reproduction uses current telemetry, one acquired/equipped item and one attack, followed by a valid empty ATTACK or ITEM_EQUIPPED frame. The reader reports UNSEALED and zero gaps; the run count is incomplete while the item row incorrectly reports recorded/exact provenance. No player file was used.

## Small correction
RowBuilders retains RunFactsBuilder's conservative incomplete provenance when constructing all count-based datasets. Original values and raw events are unchanged. This does not pretend the run began mid-run: the existing run.partial field remains absent.

Row derivation version14 becomes15 so old disposable row caches are rebuilt. Cache/container/save/raw-history formats and the separate fact-cache derivation version remain unchanged. Raw history must never be deleted to repair a cache.

## Permanent regressions
Four new methods cover healthy exact counts, unreadable attacks, unreadable equipment events, and a valid old-version14 row cache that must rebuild while preserving raw bytes. Existing damaged-history and quest-boss tests retain their assertions, updated only for the new row-builder version.

Against unchanged TEST226 production, the original three-method control produced exactly two expected failures and one healthy pass. A clean local compilation covered1597 exact production Java files (SPD/core plus the four service interfaces). Corrected provenance/cache/boss checks passed14methods. A broader130class statistics/query/archive/report selection passed661methods in18.393seconds; the14methods overlap that661 and must not be added.

Local setup corrections are recorded honestly: the first standalone compile lacked four source-matched service interfaces; an incremental overlay retained the old inlined cache constant; and the first broad run had660passes/1missing-resource fixture error. Fetching exact source interfaces/manifests and clean-compiling resolved these setup errors. No assertion was removed or weakened.

## Concurrent work preserved
Real branch commit9e553d9378dbfeca4c588910162872278ac20e2d independently adds two collection-ledger preservation regressions. TEST227 run38831758939 subsequently completed successfully. The newer saved-column correction at dba5e7478418c0657b1fff7f1c117f2eb6b1c3c4 has produced TEST228 in run38834508461; its Android verification remains pending. Both independent corrections are preserved. This query fix does not overwrite that work and is not in TEST227 or published TEST226.

After incorporating the current UimsdModel and collection-ledger production/test changes into the local fixture,131 selected classes passed670methods in18.212seconds. This is a combined local selection, not the complete Gradle suite. The failed GitHub branch-update request did not publish the earlier aa968c067151bb6df699aa85a02ca7d5e495908f candidate; it was not forced over newer work.

The next verification is core-only: reproduce the original failures on exact226 source and run the complete combined1691-method suite. It assembles no APK and publishes nothing. Android verification and the next source/history/build/signature checks remain required before this query correction is delivered in an APK. Use actual immediately preceding produced228 evidence in the next build ledger, even if its later Android checks fail.

## Remaining limits
Physical-phone/owner-save acceptance and the entire UIMSD audit remain incomplete. This fixes provenance reporting, not every possible malformed-event semantic error or individual lost-event attribution. It introduces no gameplay, Custom Rules, scoring, lore-content, storage-compression or online-service change.


## Exact core acceptance — 2026-10-16T01:40:03Z
[Run 38835333283](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38835333283) completed successfully for exact source `f0b0691362f9ef9f4e753cbc12318a857a570238`.

Downloaded artifact `12019320632` has ZIP SHA-256 `968eb92312eea1cfa8a1317288bfae45f0c887d02f634b18219ca6ea5f79713e`. Its original-source result records exact TEST226 source `a6e2f5e9aafb370ced629db01be300f6fab3f31e`, three methods, the two intended failures and one clean control, with original production unchanged. Independent corrected XML recount is 1,691 tests across 333 suites, 0 failures/errors/skips.

This validates reproduction and the complete current core suite only. It assembled no APK, ran no Android tests, published nothing and supplies no signature, application-ID, screenshot or phone evidence. The next required application gate is 1,691 core/debug/complete immediate-previous TEST228 history/33 API34/42 PNG on the exact current descendant.
