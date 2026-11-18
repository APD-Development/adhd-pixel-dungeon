# Source-bound UIMSD row-cache correction — October 15, 2026

## Confirmed cause and reproduction
Complete current-source reads of DatasetStore, RowCodec and DerivedStore found that the fact cache checked raw SHA-256 but row caches still checked only source length. A real SyntheticLog/RunLogWriter fixture replaced a valid log by another valid same-name, same-length, same-timestamp log with score47 instead of31. The current DatasetStore returned the stale31. This is a controlled reproducible integrity issue, not an observed owner-phone loss.

## Correction and compatibility
DatasetStore now fingerprints raw sources with SHA-256 and checks the fingerprint when accepting cached rows. A newly rebuilt cache is written only if source length/digest still match after parsing. Hash failures bypass the cache; parsed raw rows remain available. One32KiB buffer is reused across source fingerprints within a Catalog load rather than allocating one per log.

RowCodec adds source-bound derived format3 with32fingerprint bytes; its existing generic format2 API remains available. Production DatasetStore requires a bound cache, so old unbound caches rebuild once and then remain warm. Generic decoding supports2/3; the CRC covers the new source identity too. Raw logs, archive format, game saves, existing fields, scoring, loot and difficulty limits are unchanged. No player history is deleted or rewritten. This changes disposable derived cache metadata, not production raw-history encoding or compression.

## Tests and limits
Four new permanent methods cover same-name/length/timestamp replacement with actual new score, unchanged byte-identical cache/raw reuse, valid old format2 rebuild/warm reuse, and fingerprint mismatch/unbound/malformed-identity rejection. Original reproduction failed: expected47, got31. Corrected13focused new/existing tests pass; final buffered version13pass0.959s. Earlier10pass0.658s. Broader617recording/query/archive tests passed25.132s before the allocation-only buffer reuse; local source guards use baseline121. These are not a substitute for full current-source checks.

Initial local broad invocation omitted three existing compiled fixture classes; explicit compilation fixed the harness setup, with no assertion weakened. Current DatasetStore/RowCodec and new regressions compile locally. Full expected1626core/debug/history/27API34/35PNG remains pending at request time. This batch does not claim a delivered APK.

## Performance and remaining audit
Source validation requires sequential hashing on a warm load, and a second hash when rebuilding to detect an observed source change. This costs additional IO/CPU; no performance gain is claimed. Hash workspace is bounded32KiB per Catalog load plus digest state, and metadata increases32bytes per cached log, not per turn. No raw-history optimization or lossy conversion is introduced. Separate instances still use fixed optional-cache temporary paths; controlled cross-instance cache-write investigation remains a separate item. Full211component audit is not declared complete.

## Earlier independently rechecked application evidence
Earlier compiled APK is Codex TEST199 from sourcefe79acbc87d84438a8428d81a43849643844baea/run38722338971. Downloaded unique XML:1619core/317suites and27API34,0failure/error/skip.35PNGs decoded320x640 and contact sheet reviewed. APK artifact11966397306 ZIPdigest1af114733c71c9c4e4f6c91c19a2f43b0cf60968fb858b5d091bdbca1339cc5b; local binary checksum/signing comparison not performed for that staged APK. CoreZIP9870033b9010f737053c4513db762635db69a4104d675d2ec948e7f61d3bc3fc; AndroidZIP2f286f04ebad7188a2f472cb5e567881096b914b12ff5fffb04fd15235c4947c.

TEST198 remains actual owner-delivered, signing/application-ID/checksum-compared fallback. All18controls remain enabled for new runs; active rules locked and earned scoring unchanged. No phone/tester-distribution claim.

## Latest predecessor and integration
While the branch write awaited completion, verified TEST205 landed. The expected-head update was rejected and no history was overwritten. The backward-compatible RowCodec API was integrated first (0a60c741), then DatasetStore (b80658be), then the four permanent tests (e44c887f) using current file blob guards.
Immediate prior actual APK is now Codex TEST201: sourcea5071e47270ef3953a85eecc50f285d617f96fb3/run38736265505. The handoff records1622core+27API34/debug/history/35PNG passed and APKsha42075e16015fe677f1baf20bf946d60dddf2018143007950cdbf90ed63b4f692. Preserve that newer boss-phase telemetry and use it for the next build comparison, rather than repeating its changes under this cache fix. Expected next core count1626. The separately published owner fallback remains Codex TEST198.


## Full-gate outcome

Exact workflow head `cef68f0f80b1dd0c3e1660e53e1c0a0dad2e9e7e` passed [run 38742449533](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38742449533) from 2026-10-15T13:24:39Z through 13:38:06Z.

- Core artifact `11975569462`: 1,626 tests in 320 XML suites, zero failures/errors/skips; GitHub digest `sha256:debd3057f8d03abd61683314f517d5d7e77cdbf648e24fe20e1041f0695bc1d3`.
- APK artifact `11976650506`: debug assembly and complete immediate-previous compiled TEST205/Codex TEST 201 history passed; GitHub digest `sha256:37675d2513f8e1cc88aa4f95fb06c2b519130cc79e5b26aee431860c370898ba`. The contained Codex TEST 202 APK SHA-256 is `cab516793cedf03ca90b04e6de778def9cb8fd1525d8e86bf98859cf47b7b519`.
- Android artifact `11978246008`: 27 API34 methods in 12 XML suites, zero failures/errors/skips; 35 PNGs all decode as 320×640 and the complete contact sheet was manually reviewed clean; GitHub digest `sha256:e29b5ad73656ca31b0c0a2050193df4dfc269fff692a0d3a47d9d715fba4141a`.
- Private delivery remained skipped. No physical-phone claim. Later Cleric-lore branch commits are not included in this evidence.
