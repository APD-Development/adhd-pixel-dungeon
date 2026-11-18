# TEST106 legacy statistics version safety

Confirmed APD UIMSD compatibility bug: DerivedStore.readLegacy decoded records, totals and daily scores from newer telemetry, while RowBuilders.fromLegacyLog correctly refuses that unsupported schema. A future-major or future-minor log could therefore appear as current statistics in the legacy totals path and override a valid totals snapshot. Both are reproduced using real encoded log files; no actual owner future-format data is claimed.

The legacy fact reader now applies the existing header/newerThanReader refusal policy before interpreting events. Unsupported raw logs stay byte-identical and remain stored; supported imports are unchanged. DerivedStore DERIVATION_VERSION5→6 invalidates caches that previously retained those misinterpreted facts. Cache FORMAT2, telemetry/container formats, raw data and game saves are unchanged; there is one cold fact-cache rebuild after upgrading. This is not a deletion or rewrite of player history.

Three new permanent regression cases plus28 existing derived-cache/checksum/text/version/legacy-event cases pass31unique0.282 seconds. The three cases fail against exact preceding production (0.105 seconds), including an affected derivation5 cache incorrectly being trusted. Tests cover future major/minor logs, records/totals/dailies, supported imports, buildLegacyOnly, cold/warm cache loads, old-cache rebuilding and byte-identical source files. Full1293core/21API34/20PNG/debug are pending; signing/private delivery pending. No physical-phone or owner-save verification is claimed.

Potential regression is incorrect exclusion of supported imports or cold-cache cost. The version rule matches the existing main dataset policy; historical supported import and cache tests pass. Downgrade-to-old-build future data was the simulated trigger; no general claim that all future schemas are supported is made.

