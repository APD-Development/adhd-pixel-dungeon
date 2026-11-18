# TEST104 UIMSD comparison integrity

## Confirmed fixes

RowMerger's ANY branch always labeled a merged false value as recorded, ignoring the provenance of its false segments. A real item seen but never recorded equipped in a partial resumed segment became an exact 'never equipped' answer after combining logs. This also made equipment-use rates count uncertain observations. The false branch now retains the weakest false provenance using the same existing ranking as other merges. Positive/unknown branches and raw values are unchanged. Three permanent regression tests cover all provenance classes, rate exclusion, actual partial log encoding/reading, cold and cached DatasetStore loads and byte-identical raw records. All three fail against the actual preceding source (0.154 seconds), proving the issue.

PerUnit's denominator key omitted the lower-bound marker that QueryEngine includes in grouped numeric keys. Exact depth10 and at-least-depth10 groups therefore overwrote one another in the denominator map. Added that same marker to PerUnit's key; no new filters, operators, limits or data formats. One permanent regression checks separate floor, run, turn and 1000-turn denominators. The preceding source fails the floor case (expected1, actual2;0.054 seconds).

## Local verification

Four new cases plus74 existing cases across RowMerger/overflow/QueryEngine/floor comparisons/reports/DatasetBuilders:78 unique tests passed0.583 seconds after the final minimal patch. Raw files remain byte-identical; per-log row cache format is unchanged because merged rows are recomputed on every catalog load. No stats deletion, save editing, gameplay changes, scoring changes, new caps or online activation.

Full1286core/21API34/20PNG/debug, signing/applicationID/checksum and private delivery are pending at the source commit. Host tests are not Android or physical-phone evidence. Potential regression: knowledge labels or per-unit denominators; exact/legacy/inferred/incomplete values and existing floor/report tests cover these paths. Further numeric lower-bound group drilldown behavior is under investigation, not claimed fixed.

