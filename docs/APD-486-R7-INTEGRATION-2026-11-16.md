# APD486: complete C466R7 + C485 integration

## Owner authorization and isolated claim
Started 2026-11-16 11:03 Eastern. Owner explicitly requested: get all of 466r7 into 485/486, after the read-only audit proved omissions. This is a one-session integration authorization, not permission to resume recurring development or alter other games/tasks.

Isolated branch: `chatgpt/apd486-complete-r7-integration-20261116`.
Verified authoritative starting head: `18c8c3e74209fcaa6434399f5bbb681214455733`.
C485 frozen production source: `c266b30559a7427b435749837006907ec1a682f6`.
Exact C466R7 producing source: `34a1471ccd2f56ee21bb8c2579cb154ceb5e21c7`.
Common ancestor independently returned by GitHub: `8c74997210585191d12603ad5e2ee398b1c54fe7`.
R7 build run/artifact: `43769708853` / `14184236258`.
C485 build run/artifact: `43897257844` / `14238519767`.

## Scope and implementation plan
- Preserve every R7 production delta, not only the seven weapon/shield classes. Include FullPackSwap, ThrowAim, GreaterMetamorphosis/recipe, talent changes, adaptive search/room rules, playback recovery, descriptions, catalog/generation registrations, sprite assets, and their existing tests.
- Preserve all later C485 changes, especially rational SeedReuseScore v6, writer reopen identity-only reads, history import/read allocation and I/O optimizations, diagnostics and save/history integrity guards.
- Enumerate every R7 changed path against the actual common ancestor. Perform a three-way merge, inspect every overlapping path and conflict, and record resolution reasons. Do not blindly replace C485 with the older branch or merely cherry-pick named features.
- First reproduce the missing-feature failure against frozen C485. Add a permanent completeness regression. Run the complete combined core suite and desktop/Android debug/release/instrumentation Java compilation; retain failed attempts and do not weaken existing tests.
- Use original private signing inputs only through the established build environment. Preserve package IDs, versionCode 920, signer fingerprint, edition boundaries, saves, history and approved numerical rules. Build distinct C486/P486 candidates only after source tests pass, with matching source and checksums.
- Verify APK class/asset contents against R7 and C485 and record exact differences. Native Android acceptance, visual acceptance, install-over/save checks and publication remain separate gates; never label static or compilation checks as native acceptance.
- Recheck live remote heads before any shared-branch integration. Use expected-head non-force updates; preserve all original branches, candidates, releases, history and evidence.

## Coordination
Read AGENTS.md, repository handoffs/known-issues ledger, the frozen C485 continuation, the audit report, live refs, open pull requests and original artifact metadata before starting. Existing C480/P480 native work (#211/#212), C485 build-only PR #226 and all earlier independent work remain untouched. No recurring schedules are changed. This claim covers only reconciling the exact R7 and C485 source lines and validating/building their combined successor.

## Verification status
Pending. Branch creation and claim are not source integration, successful tests, a new APK, native acceptance or publication. Record only outcomes actually observed below.

## Completed verification superseding pending status

Recorded 2026-11-16T11:28:02.066659-05:00. Exact combined source `c6fa033167789ef9f2fc6f31bcff39c824dfb4db` passed 3,540 core tests / 539 suites and all requested Java compilation. Original-signed C486/P486 APKs are retained in artifact 14244034917 and were independently checked after download. See `APD-486-R7-INTEGRATION-VERIFIED-2026-11-16.md` for full source, test, APK hash and limitation receipts. Native acceptance and publication remain unperformed. No recurring tasks changed.
