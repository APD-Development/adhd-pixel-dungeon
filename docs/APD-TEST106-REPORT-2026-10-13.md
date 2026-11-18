# TEST106 legacy statistics version safety

Confirmed APD UIMSD compatibility bug: DerivedStore.readLegacy decoded records, totals and daily scores from newer telemetry, while RowBuilders.fromLegacyLog correctly refuses that unsupported schema. A future-major or future-minor log could therefore appear as current statistics in the legacy totals path and override a valid totals snapshot. Both are reproduced using real encoded log files; no actual owner future-format data is claimed.

The legacy fact reader now applies the existing header/newerThanReader refusal policy before interpreting events. Unsupported raw logs stay byte-identical and remain stored; supported imports are unchanged. DerivedStore DERIVATION_VERSION5→6 invalidates caches that previously retained those misinterpreted facts. Cache FORMAT2, telemetry/container formats, raw data and game saves are unchanged; there is one cold fact-cache rebuild after upgrading. This is not a deletion or rewrite of player history.

Three new permanent regression cases plus28 existing derived-cache/checksum/text/version/legacy-event cases pass31unique0.282 seconds. The three cases fail against exact preceding production (0.105 seconds), including an affected derivation5 cache incorrectly being trusted. Tests cover future major/minor logs, records/totals/dailies, supported imports, buildLegacyOnly, cold/warm cache loads, old-cache rebuilding and byte-identical source files. Full1293core/21API34/20PNG/debug are pending; signing/private delivery pending. No physical-phone or owner-save verification is claimed.

Potential regression is incorrect exclusion of supported imports or cold-cache cost. The version rule matches the existing main dataset policy; historical supported import and cache tests pass. Downgrade-to-old-build future data was the simulated trigger; no general claim that all future schemas are supported is made.

Last deliveredTEST105: sourcec15313c6d128bcd3557ff8df0b49ffe24258c8d5, verification38357456703, delivery38358838879/cee5af337ab69e265956c4a4c29212ff6b01fcca, published2026-10-13T06:49:22Z. Actual1290core+21API34, zero failures/errors/skips,20PNG/debug; native group-sheet touch helper passed. AppID/signature compatible, APK SHA25644fd886d015596e2dd325018030651096ed5be85feca0061f916405ab2957580. Install over; never uninstall or clear data.

Current UIMSD audit inventory is pinned to exactTEST105 Git blobs:211production components/35013physical lines, including backend/community interfaces;68reviewed so far. Remaining components and game integration hooks are pending. This is a current scoped count, not the unverified original Claude line count, and the complete audit is not finished. Fixed-slot metamorphosis's already-owned interaction remains under review; other backlog incomplete. No continuously running Codex worker is claimed.

## Completed verification

Exact source015d0392f5ff607746a6db0a2879fd4bbd4c3c45, run38359186475 succeeded: started06:52:13Z, ended07:04:11Z. Downloaded XML1293core+21API34, zero failures/errors/skips;20PNG/debug. Existing-artifact signing/appID/hash/private delivery now staged, not yet published. Physical-phone and owner-save verification absent. Fixed-slot metamorphosis work is local7targeted passed and NOT in106; its native checks/build remain pending.
