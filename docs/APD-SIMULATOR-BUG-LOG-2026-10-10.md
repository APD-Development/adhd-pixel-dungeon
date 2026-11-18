# Simulator/delivery investigation log — October 10, 2026

| Issue | Location | Evidence and cause | Severity/status | Phone test |
|---|---|---|---|---|
| Lost episode JSON after successful connected tests | tools/run-warrior-simulation.sh | Gradle uninstalled app before run-as extraction; source3402's 68-byte file was an error, not JSON. Scheduled d595 uses ordered Base64 chunks;9d0 parser rejects malformed/empty evidence. Actual0a episode JSON parsed. | High for simulator; fixed and exercised | No; disposable emulator |
| Waiting for clock advance after every action | WarriorSimulationDeviceTest.perform | Source79cef trace reachedfloor2 then EXPLORE readiness timeout. Hero normal blocked/interrupted moves can release input without spending turns. Exact rejected target absent, so specific trigger remains unknown.0a checks queued action/input release and adds diagnostics. | High for simulator; corrected wait passed one episode; monitor batches | No; not claimed a player-game bug |
| Fixture inherited shard seed | tools/test-warrior-instrumentation.py | Shards1–4 failed before gameplay; outerseed1000020 reproduced fixture mismatch.5541 explicitly isolates seed;6 guard cases pass under outer1000080. | High for batch execution; fixed; four shards started | No |
| Unsupported Albino/Wraith bounds | AuditedSewersMeleeBounds / WarriorSimulationDeviceTest | Real shard2 seeds1000042/43 aborted on class guard; only four attempted, two complete. Audit confirms Albino inheritsRat1–4; Wraith stored-level rawmax2+level. b6e0 adds exact audited classes and source/RNG checks; raw limits exclude status damage. | High for simulator coverage; unit tests pass; corrected native/batch pending | No; no production enemy change |
| Test-only edits falsely invalidate existing APK source reuse | tools/apd_existing_build_evidence.py | Top-level core/android tree SHAs change when only test files change. Retained red/green regression: recursive mapping ignores only known test/doc/tool/workflow paths, protects all production/assets/build files and refuses truncated trees. Real1483 versusb6e0 comparison:2197 protected files, zero differences.11 local tests pass. | Medium for daily delivery efficiency; fixed locally, committed with this log | No; signature/checksum/package checks remain required |

| Unclassified gameplay window on seed1000042 | WarriorSimulationDeviceTest.perform | Run38083672601 artifact11681815395 reconstructed one real342-action trace, then stopped on an open window at floor2/HP19/position505/actor time278.0. Current boolean-only guard cannot name it. Add exact window/stairs/terrain diagnostics before deciding whether dismissal is safe; do not count the episode. | High for simulator coverage; diagnostic rerun requested | No; not a confirmed player-game bug |

No zero-bug claim. Runtime stalls, optional dialogs and other unaudited encounters remain investigations until actual traces establish their cause. Keep every failed attempt/source manifest; do not count missing episodes or synthetic fixtures as played runs.

## Consolidated original batch, 2026-10-10 20:45:46 UTC

Five shards retained62 unique actual attempts,43completed and57requiring recovery. These are early episodes, not full wins or causal balance findings.

- Confirmed adapter omissions: GnollExile/Mimic aborted20/22/32/34/66/80/82; new exact-class audit and inherited/actual-roll tests cover them. Unknown subclasses still refuse. Severityhigh for simulation; nativepending.
- Navigation:7/8/35/89/97 made no state progress; original traces lacked target. Controlled test reproduces choosing an occupied NPC tile with terrain-only mask. New observed-occupancy mask and explicit ordinary movement prevent accidental contextual NPC/chest actions; exact original per-seed causes await native command traces. Severityhigh adapter; not confirmed player-game faults.
- Optional dialog:60 and b6smoke42 stopped on an open Window. Preserve scheduled4ec8 class/position/terrain diagnostics; do not make unidentified choices. Nativepending.
- Failed pickup retry: Hero.actPickUp can refuse full inventory/full-health dew without a turn; adapter repeated taps forever. Retry only when item/HP/inventory changes, never discard anything. Player mechanics untouched; nativepending.
- Failed smoke evidence only existed in logcat because wrapper exited before decoding. New parser preserves validated incomplete attempts separately, still exits failure; permanent negative case passes. No failed episode becomes passed evidence.
