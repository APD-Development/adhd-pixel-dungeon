# APD TEST70 development and historical change audit — October 12, 2026

## Candidate and verification
The verified fallback candidate is TEST70; its actual source, run, test counts and signatures are recorded in the generated verification section.
TEST70 includes the source-backed historical reconstruction from TEST68 plus the owner's hotbar-settings correction. Its exact application source and CI times are populated by delivery evidence.
TEST68 historical-reconstruction source: 53276e8706f23017b23eb624643d67f60c0f4603.
Failed TEST69 source:88e73db126152e3d779603cd989473910de972e9, run38261032941.
TEST70 application source:445c47fb1fe21b86055260fe3f34468cc22c51e1, run38262103238.
TEST68 verification/build run: 38259755048. The delivery verification section is generated from actual successful CI, XML, APK checksum and signer evidence; a failed candidate must not be published by this workflow.
TEST67 remains the last delivered fallback until the current candidate passes all gates. No physical-phone testing is claimed.

## TEST70 changes compared with the preceding produced APK, TEST68
Removed the APD hotbar layout and food-placement settings, preserving saved data and directly exposing Cleric quick-cast button settings. Extended the existing real-touch Android check to confirm four Cleric options remain accessible and changing their count still works. The obsolete settings-menu method is removed; unrelated category runtime and persisted preferences are preserved.

## Historical reconstruction introduced in TEST68, compared with TEST67
Recovered offline, separately attributed change histories for every retained historical APK in the audited repository. The historical catalog contains 2,859 individual entries across 132 actual APKs, with categorized native sprites and short build-specific popups. Legacy Build, Codex TEST and Private TEST numbering remain separate.
The reviewed historical source coverage includes 1,902 changed files, including gameplay, UIMSD, UI, resources, tests, tools, configuration, workflows and documentation. Historical files are included in the APK and remain available offline.
The future build gate now checks the complete pinned Git source difference instead of omitting internal/test/documentation changes, refuses missing or duplicate descriptions, and compares every compiled historical TSV against reviewed source.
Added four permanent host regression cases covering historical identities, complete changed-file attribution, asset fingerprints, sprites, concise descriptions, duplicate/private-link rejection and large records.
Added an Android regression case for the actual Build22, Build37 and TEST67 sections, checking that all categorized icons remain present inside native scrollable sections. Build22 contains 275 entries, with no arbitrary entry cap.
Fixed UIMSD SUM overflow: extreme signed-long values could wrap and display a false exact number. Normal sums retain their fast path; overflow uses exact arithmetic, cancellation can still produce a valid exact result, and an out-of-range final result remains unknown.
Added six permanent SUM tests, including boundaries, positive and negative overflow, cancellation, incomplete provenance and 1,000 deterministic oracle sequences within one test.
Delivery tooling now derives the private release tag and APK filenames from the actual build number. The earlier TEST67 publication failure caused by stale TEST63 names was documented and corrected before TEST67 delivery; it did not require another APK build.

## Root causes
Historical placeholders and broad “since TEST50” grouping lacked per-APK attribution. Reconstruction now pins each retained artifact to its own source, package/version, checksum and immediate preceding produced APK within its lineage; release labels alone are insufficient.
One private TEST2 release actually contains TEST1. It is recorded as a delivery alias, not invented as a distinct APK.
The initial read-only historical census retrieved 99 APKs but failed 33 Actions artifact requests because the download Accept header was inappropriate. The corrected census reused successful evidence and retrieved only those 33 missing artifacts; its final error count is zero.
UIMSD used unchecked long addition in RowMerger. Reproducing the old arithmetic produced two failures in five targeted cases; the guarded implementation passed the subsequent targeted suite.

## Historical audit totals and limits
Audit scope before TEST68: Legacy Builds1–84, Codex TEST1–67, Private TEST1–3: 154 numbered records.
132 retained APK histories have complete source-backed coverage; zero retained histories remain partial or unavailable. This is source-and-artifact reconstruction, not a claim that every historical APK was playable or that all historical tests passed.
21 numbers have no retained APK; one Private TEST2 record is a TEST1 delivery alias. These are explicitly labeled and do not silently disappear.
Legacy numbers without retained APK evidence: 1,31,38,47,79,83.
Codex numbers without retained APK evidence: 4,12,13,14,15,16,19,28,30,38,52,53,56,57,59.
No known retained APK remains awaiting reconstruction. Missing retained artifacts prevent treating the listed numbers as distinct verifiable APK deliveries.
After successful TEST68 assembly, its own history is an additional current record; it is not part of the 132 historical artifacts inspected by the census.
Actual binary checks covered package/version, checksums, bundled history and artifact/source provenance. The audit did not decompile and formally prove every historical method. Historical workflow failures remain historical failures even where their APK exists.

## Tests and results
Local SUM and related RowMerger/DatasetBuilders/FloorsPerUnit checks: 51 passed; no failed or skipped cases.
Local historical coverage: four passed.
History maintenance Python checks: eleven passed, including a 450-path comparison and deliberate compiled-history corruption rejection.
Required full core suite, debug assembly, compiled-history equality and API34 instrumentation are recorded from the completed CI evidence in the generated verification section. Expected counts are not substituted for actual XML counts.
All five API34 screenshots were inspected: current TEST70, original SPD, Build22, Build37 and TEST67. The native two-column sections, gold border, grouped icons and build navigation were visually confirmed. Compilation alone is insufficient. This report does not claim physical Google Pixel, Pixel Buds or original player-run acceptance testing.

## Potential regressions and safeguards
Large historical sections could expose layout, icon or scrolling problems; host no-cap coverage and Android Build22/37/67 checks address these, with screenshots reviewed separately.
New historical claims could be attributed incorrectly if release names were trusted; artifact metadata, source comparisons and separate numbering prevent the known alias error.
Strict future source coverage can block publication when an included internal change lacks a reviewed entry. This is intentional; it prevents empty or incomplete new histories.
Extreme UIMSD SUM results now become unknown rather than wrapping. Persisted source records, save formats and ordinary calculations remain intact.

## Preserved functionality and unresolved work
No inventory capacity, pickup gameplay, save data, signing key, application ID, statistics records, enemy behavior or balance was changed by this batch. Claude's branch and main were not overwritten or force-pushed.
The TEST50 player's original run was not reproduced from a supplied save. Lost Inventory remains a possible intended restriction; no claim of a proven capacity bug or automatic expiration fix is made.
The comprehensive UIMSD audit is not complete. Broader approved UIMSD visual work and Community improvements remain on the backlog.
No online Community backend, paid service or public release was enabled. No continuous unattended Codex development mechanism is demonstrated.

## Commits and honest timing
TEST67 application source: fa76dede19aa1a207531413b9de838b1d40fefa0; prior verification run38248003026 and private delivery remain recorded.
SUM guard: c2a050cdbae78c87fda7e772ac6c9b9c5e9dbc49.
Read-only first census: 45a0811a7d8c45d206784996f1fa3714bd9903a8, run38251610456.
Corrected census: bd6f32316cebdfd07a34eb40659d7631e6b2d53a, run38252104362.
Historical reconstruction and full verification candidate: 53276e8706f23017b23eb624643d67f60c0f4603, run38259755048.
Candidate CI began October12 at14:00:33 UTC,10:00:33 EDT. Core/assembly job completed14:03:46 UTC. Final CI and private publication times come from actual delivery evidence; no unsupported overnight work duration is claimed.
Owner extended deadline:noon EDT. Install the verified private APK over the existing APD application; do not uninstall or clear data.

## Unsuccessful preparations and corrections
TEST68 run38259755048 passed all1,160 core tests, compiled the APK and matched every compiled historical TSV. API34 instrumentation ran20 tests:19 passed, one failed because the preservation tool restored the obsolete test-3 identifier, which pointed to a missing asset. The original-SPD comparison screenshot was consequently not captured, and PNG validation also failed. TEST68 was not privately delivered as playable.
TEST69 run38261032941 stopped before compilation because its gate required TEST68's overall verification success. It produced no APK. The hotbar removal source was committed but not delivered by that failed preparation.
Corrected alias handling preserves reconstructed private histories, rejects unresolved missing targets, and has two permanent regression tests. Historical preservation now requires successful source core/assembly evidence for a preceding produced APK; current publication still requires ALL current core, assembly, Android, checksum and signing checks to pass. This preserves exact APK attribution without hiding failed verification.

## New approved requests still pending
Enhanced Tile Visibility and automatic discovered-stair navigation are approved additions, not implemented in this candidate. They require visual/gameplay interruption regression checks before delivery; local implementation is in progress and excluded from TEST70. No unfinished shortcut, rendering change or speculative history entry is activated.
