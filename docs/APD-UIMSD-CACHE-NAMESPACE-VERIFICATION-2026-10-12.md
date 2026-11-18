Confirmed UIMSD cache namespace defect, prepared locally; not committed or delivered yet.

DatasetStore.rowsFor used derived/rows/<id>.apdr for both runs/<id>.apdlog and legacy/<id>.apdlog. RowCodec validated only byte length/builder/registry/checksum, not the source folder. Distinct valid same-name/same-length recorded and imported-ranking logs therefore reused each other's decoded rows.

Reproduced with actual append-only writers and actual ArchiveExporter/ArchiveImporter round trip. Both archived files accepted and restored byte-for-byte, then old DatasetStore lost the distinct imported-ranking row by reusing the recorded-run cache. Baseline failed the exact imported-row assertion in0.275s. No altered generation/RNG, fake bytes or corrupt cache required.

Fix namespaces the derived filenames as runs-<id>.apdr versus legacy-<id>.apdr. Both prefixes change so previously ambiguous caches are ignored and rebuilt. Raw logs/game saves/formats unchanged. Old derived files are not deleted by this change and may take extra disk space until normal cache cleanup; safe but not a claim of reduced storage use.

Permanent regression verifies both records survive cold/cache loads, archive raw files stay byte-for-byte intact, both correct caches reused, and an obsolete ambiguous cache cannot affect the reload. Final targeted suite:69unique cases across new namespace, checksum, UTF-8, allocation-bound, dataset builders, row merger and archive roundtrip tests passed. Android/full build/release pending. Existing cache tests are preserved without weakening assertions.

Reachability: archive path validation accepts both namespaces independently with plain valid IDs. This is a valid imported-history edge case, not evidence that the owner's current history encountered it. No record is deleted or repaired in place.
