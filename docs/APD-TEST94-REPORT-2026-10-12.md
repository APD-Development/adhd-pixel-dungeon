# UIMSD imported-history cache fix (next expected TEST94)

Last verified delivered APK:TEST93 sourceaebb1abf66755c87c175b5663af15586a628e4d5,run38330992144,private delivery38332088864 at01:26:28UTCOct13=9:26:28pmEDTOct12.1250core+21API34,18PNG,zero failures/errors/skips,compatible TEST67 signing/appID. APK SHA2565d96b1d4ab6c7a42922183f6a9a0e970103968fabe58c819283b9d19495a1387.

Confirmed root cause:DatasetStore used the same derived filename for runs/<id>.apdlog and legacy/<id>.apdlog. Distinct valid source logs with equal ID and byte length could pass cache validation and substitute recorded rows for imported-ranking rows. Reproduced through actual append-only writers and ArchiveExporter/ArchiveImporter. Both files restored byte-for-byte but oldDatasetStore lost the imported ranking; baseline regression failed0.275s. This is a valid archive edge case, not a claim about the owner's exact history.

Fix uses runs-<id>.apdr and legacy-<id>.apdr. Both prefixes change so old ambiguous caches cannot be reused; originals are read to rebuild derived rows. No game-save/raw-log/serialization/calculation changes. Old cache files remain on disk until normal cache cleanup; additional one-time rebuilding/disk use is possible. Do not claim this reduces storage.

New permanent regression verifies both records, source-preserving archive transfer, cold/warm loads, separate cache hits, ignored obsolete ambiguous cache and unchanged raw bytes.69unique related cases pass1.372s after final changes. Modified production classes compile locally. Full1251core+21API34+18PNG/debug checks are pending at this source commit; no verified/delivered94APK claim.

Also removed WndUimsdRow's unused private singular helper (existing DatasetDef labels remain) and RowMerger's assigned-but-never-read counter. No public API or displayed fields removed. Existing tests/assertions preserved.

Risks/limits: one-time cache rebuild, residual old derived files, emulator/full build still pending; no physical-phone/owner-save testing. The complete UIMSD audit remains unfinished. A separate100000-seed TEST79 armor-generation study is running locally; no final probability claimed here. Its data is isolated and is not added to player statistics.
