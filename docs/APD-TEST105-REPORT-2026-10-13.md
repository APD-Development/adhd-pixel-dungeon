# TEST105 exact group drilldown

## Reproduced APD UIMSD bug

QueryEngine deliberately separates exact numeric values from lower bounds and groups text case-sensitively. UimsdModel.openGroupRows previously converted all known group keys into ordinary EQ filters. Ordinary numeric equality asks whether the underlying true value is equal (so lower-bound records cannot be included); enum equality ignored incomplete provenance; text equality is case-insensitive. As a result, opening an at-least10 group could list exact10 records and omit the group's own records, while opening a case-sensitive or incomplete enum group could include another group's records. Three old-source regression cases reproduce all modes (0.033 seconds).

Added three narrowly scoped drilldown predicates matching the existing group keys: non-bound numeric/enum value, bound value and exact-case text. They are excluded from Op.ops(), so ordinary player filter choices and their behavior remain unchanged. Unknown groups still use is-unknown; hidden/unfinished guard checks still run before matching. UimsdModel uses these predicates only when opening grouped records. No statistics recalculation, raw records, gameplay, saves, new limits or online activation.

## Compatibility and verification

QueryCodec already serializes operator names; no format/parser change is needed. Existing saved queries remain supported. Newly saved drilldowns retain their exact selection across encode/decode and actual SavedViews file roundtrips. Older builds that do not know the additive predicates reject that query instead of silently dropping its filters; the existing saved-view loader preserves unreadable queries. Game saves and raw telemetry formats are unchanged. Existing11SavedViews tests pass0.121 seconds, including preservation of future/unreadable queries.

Four new regression cases plus71 existing query/model/disclosure/grouping/boss-scope cases pass75unique1.564 seconds. The first broader local run failed one source-introspection test because its required apd.query.src property was omitted, not because product behavior failed; corrected actual source fixture/property passed without weakening assertions. Tests cover each group's exact members, case variants, empty/unknown values, enums, lower bounds, query persistence, back navigation and unchanged ordinary operators.

Existing Android every-screen test strengthened to open native group sheets for numeric, enum and text cases, scroll to the real Open-records action and tap it through PointerEvents. It asserts correct membership and Back restoration; action text fits the native window. This is added coverage inside an existing method, not three new Android tests. Full1290core/21API34/20PNG/debug are pending; Android compilation and this new touch helper have not yet been verified. Physical-phone and owner-save testing absent.

Potential regression is drilldown predicate validation, query persistence or native modal touch selection; existing privacy checks are preserved. The complete UIMSD audit and other approved backlog remain unfinished. Fixed-position metamorphosis's already-owned-choice interaction is awaiting the explicit design answer. No continuously running autonomous Codex coding mechanism is claimed.

## Last delivered build

TEST104 source946704b57fac583721449fe95b4559d6716c36b2, verification38355939388 passed1286core/21API34/20PNG/debug. Delivery38357194744/commit e55ed3d564c4baae657c84227a8da2d711c3b197 published2026-10-13T06:32:20Z. Signer matchesTEST67, appID com.charltonchris29.adhdpixeldungeon.indev; APK SHA25674ff6492ac83acb062d27925d5d3ca44941e293e938d2223c4333269e79a5d28. Install over existing APD; never uninstall or clear player data. TEST105 private publication/signing remain pending.

## Completed verification

Exact sourcec15313c6d128bcd3557ff8df0b49ffe24258c8d5, run38357456703 succeeded: started06:34:21Z, completed06:46:29Z. Downloaded unique XML1290core+21API34, zero failures/errors/skips;20PNG/debug. The new native numeric/enum/text group-sheet pointer helper ran inside the existing every-screen method and passed. Private existing-artifact signing/appID/hash/delivery staged, not yet published. Physical-phone/owner-save evidence absent. Future-legacy-version fix is local31targeted passed and NOT included105.
