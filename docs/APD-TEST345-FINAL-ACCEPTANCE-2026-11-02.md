# APD TEST345 final exact-APK acceptance — 2026-11-02

## Disposition

TEST345 is accepted as the latest **private emulator-verified candidate**. It is not published, distributed or physical-device verified. The latest actual private publication remains TEST341 release 428527455; TEST330 remains the preserved fallback.

The accepted application is the unchanged TEST345 developer APK produced by run 41344574181 from application source `207ed6819ed2a36cd4f6ccd02eaca5d7b9aa1457`:

- raw APK SHA256: `a6c5653d5eaf182c65a5352146ce8660cdfd0f71ad70796049a87dc35b3e5b46`
- producer artifact: 13097999436
- application rebuilt during final acceptance: **false**
- application inputs unchanged: **true**
- acceptance test source: `7502c288930a125c3d304c8d5d421e057334f524`
- physical device: **false**
- published: **false**

The matching player run 41344574193 had already passed its paired checks; player APK SHA256 is `a910500b72f77906827cdf699da69b7f1151ebdb52f85b941bc6adc8cd6b7bda`. All six fresh display profiles had already passed.

## Complete API34 result

GitHub Actions run 41470277645, attempt 2, job 125363526637 ran the current complete API34 acceptance list against the exact producer APK. It compiled and installed instrumentation only and asserted that no replacement application APK was built.

- job start: 2026-11-02T15:21:40Z
- complete exact-APK step: 2026-11-02T15:23:45Z through 2026-11-02T15:35:40Z
- instrumentation duration: 615.958 seconds
- result: `OK (67 tests)`
- TestRunner result: 67 run, 0 failed, 0 ignored
- Privacy restoration: `privacySubflowsRestoreExactLocalStateWithoutQueueingOrSending` passed
- warm 72-run UIMSD Home: both openings passed the unchanged 250 ms limit
- visual evidence: 92 PNGs, each decoded and validated at 320×640
- evidence artifact: 13155976745, 7,377,011 bytes
- artifact ZIP digest: `dae59e8d8176cf388b4609b375f97073eb2c20f84581a4a97838b63569527151`

Warm opening measurements from the complete run:

| Opening | Total | Down dispatch | Input dispatch | Scene observed | Scene→Home | Input→Home | Create | Rebuild | First Home draw |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 204 ms | 27 ms | 45 ms | 108 ms | 96 ms | 158 ms | 1.410 ms | 81.581 ms | 96.826 ms |
| 2 | 116 ms | 17 ms | 28 ms | 73 ms | 42 ms | 87 ms | 2.570 ms | 28.643 ms | 42.196 ms |

## Retained diagnostic history

No failed evidence was discarded:

- Run 41347351842 was cancelled after 20 minutes with no UIMSD milestone because the diagnostic workflow omitted KVM setup. Its artifact 13100044452 has digest `845acac36a69f528ccebbad349eb83f88c49e365a9f687d796fa9897689ccae1`. The emulator showed system-service ANRs, multi-second GC and severe CPU pressure; this is an infrastructure-definition failure, not an APD timing result.
- Test-infrastructure commit `1c71f792fe97d3a6a182c5e68ab21458f77269ad` added the proven KVM setup without changing application inputs.
- Focused exact-APK run 41469223189 executed `OK (1 test)`. Its workflow wrapper concluded failure after the green instrumentation output, but the retained timings were 210 ms and 101 ms, both below 250 ms. Artifact 13154167715 digest is `f91465851c39c0fca338da088b8e2b9b3219bf62431840e63c806b9ab6b4ed8c`.
- Test-infrastructure commit `7502c288930a125c3d304c8d5d421e057334f524` added exact-existing-APK mode to the normal Android acceptance helper and the complete API34 workflow. The ordinary producer path is unchanged.
- Run 41470277645 attempt 1 stopped before emulator creation when the Android SDK supplied an unknown/corrupt system-image archive. No APD test ran. Its 399-byte metadata artifact 13154459341 has digest `7d759761b81fd048bd0600a8fd7637f9cd559974bb36b1ded6a5ef5f5f848c84`. Retrying that exact failed job once produced the terminal green result above.

The earlier 276 ms observation was not reproduced in either accelerated exact-APK execution. With four measured openings across the focused and complete runs (210, 101, 204 and 116 ms) and no application change, no production performance defect is confirmed.

## Preserved boundaries

No application source, save format, permanent statistics, scoring or scoring history, all18 rules, combat, RNG, signing certificate, application ID, version, compiled APK history, publication or distribution changed in this acceptance package. The executable build-request blob remains `3fe0ce4437fb3129a69588b2f943b1cc368c691d`; it was not edited for verification.

A separate authorized publication task may publish the already-built TEST345 pair and produce a receipt. Until an actual receipt exists, TEST341 remains the current named publication. Physical-device acceptance remains separate.
