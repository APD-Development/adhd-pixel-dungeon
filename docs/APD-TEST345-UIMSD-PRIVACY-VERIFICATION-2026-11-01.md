# APD TEST345 UIMSD Privacy Restoration Verification Report

**Recorded:** 2026-11-01T20:26:24Z  
**Approved branch:** `codex/apd-phone-fixes-2026-10-09`  
**Candidate producer head:** `207ed6819ed2a36cd4f6ccd02eaca5d7b9aa1457`  
**Application correction:** `c501320ccdcbeb15659f7de228771180779cfcae`  
**Corrected test/workflow head:** `4dab228a8cf681f64111a605adcf0cf269677f19`  
**Disposition:** rejected overall; Privacy correction accepted by focused exact-APK evidence; private artifacts only; not published or distributed

## Bounded correction

Five UIMSD Privacy subflows now carry exact scene-reset restorers: the pending evidence-request list, evidence decision, community-vote offer list, in-progress vote selection and account export/delete confirmation. The vote restorer defensively copies its selected set. Restoration does not call evidence, ballot or account actions and does not connect or send.

The API34 fixture uses an isolated temporary history root with one synthetic evidence request and one synthetic three-option vote. It restores the real production windows across repeated scene rebuilds, requires the exact evidence/vote labels and two checked vote options, compares the evidence and vote files byte-for-byte, requires a zero-item submission queue and verifies that no session file appears. The synthetic temporary files are the only files it removes.

## Actual evidence

| Gate | Actual result |
|---|---|
| Source guard | Core-only run [41319425592](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/41319425592), job `124886277582`: 425 suites, 2232 tests, 0 failures/errors/skips; all 6 RunTurnWindow methods |
| Producer | TEST345 run [41344574181](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/41344574181), job `124961552996`: reviewed TEST344 history, complete core, debug assembly and compiled-history verification passed |
| Developer APK | Exact private APK SHA256 `a6c5653d5eaf182c65a5352146ce8660cdfd0f71ad70796049a87dc35b3e5b46`; artifact `13097999436`; ZIP digest `4c615168f2df246d43f347f3f52946200f8c383d6dd2b7413664c8810356ff8f` |
| Core artifact | Artifact `13098308673`; ZIP digest `f4297e1f69d1287825223cdab0e29ce48dccc4260a7a946e949106300ebce4f5` |
| Player companion | Run [41344574193](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/41344574193), job `124961552818` passed; APK SHA256 `a910500b72f77906827cdf699da69b7f1151ebdb52f85b941bc6adc8cd6b7bda`; artifact `13098508299`; ZIP digest `a52604730babbc1563affe6ce927042eed728199b8591821fe326878a1ed8c0d` |
| Full API34 | Job `124962334977` executed 67 methods: 65 passed and 2 failed; 92 PNG captures were present and validated; artifact `13099775216`, ZIP digest `d61376d928dd9f26991f3e26c9f90b5ce711c860c26a95a252503255ab23df65` |
| Privacy first result | The production list rendered the correct player label `Run restore-fixture-run`; the new test expected the raw ID and failed. Test-only commit `586a71620bc459ded9dd19668826070290c22390` corrected both expectations; no application input changed |
| Focused exact-APK Privacy retest | Run [41346533622](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/41346533622), job `124967431276`: exact TEST345 APK verified, application inputs unchanged, instrumentation-only compile, API34 `OK (1 test)` in 3.465 s; artifact `13099059654`, ZIP digest `6a97f7644a50ced03dc3a1859c6ed50ba2bfa52521d0d70b8a3131f266e31deb` |
| Remaining API34 failure | Existing `AndroidBackDeviceTest.warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms` measured 276 ms on opening 1, exceeding its 250 ms limit. It was retained and not rerun for green hunting |
| Display matrix | Run [41344574177](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/41344574177): all six fresh profiles passed—compact, small, square, M53, short-landscape and RedMagic representative API36 |
| Display artifacts | compact `13098579274`/`b91364c4471b0ce34c195153f9654d76cd3466c8bfdfa7430bee8a43b500117e`; small `13099310619`/`5576a9ea4c75078baa58d83e508199688d121066bfe55e0b3caf1c10744c8ec8`; square `13099505313`/`34d2febf01a06a7cda32c0185f93892659b4102aecaea66715d26f0a43112003`; M53 `13098852227`/`0f1b317ba8dcc02dcde5a2566dc3bfcd7eef24473b6ca6e61110ee9cc3e521ed`; short-landscape `13099140577`/`f6b1ee9ddd431afd28557f0bfbb886601585b4b429672228b5c1827ba29885ce`; API36 `13098803204`/`89feaedd98f550cdfe5de21abbae878f86e8e14531f62b4ccdcf6f9e6dcf3545` |

The first focused run [41346189368](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/41346189368) also produced `OK (1 test)`; its job failed only because a multiline post-test Python validator was split by the emulator action. Workflow-only commit `4dab228a8cf681f64111a605adcf0cf269677f19` replaced that validator with direct shell checks before the successful rerun. Its retained artifact is `13098864484`, ZIP digest `530c25ce8fec187bec57750f7af0a29c37eb7efeacd1ec969bbfa35ef1ed2508`.

Automatically triggered staging, legacy hotbar and legacy death-order jobs again failed their old source/artifact guards and supplied no candidate acceptance evidence. Their failures were retained.

## Current APK status

The latest actually published private pair remains TEST341, release [428527455](https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/tag/apd-alpha-preview-341), published 2026-10-30T00:24:54Z and last updated 2026-11-01T03:49:33Z:

- Developer SHA256 `ad6281b3e41fe414187ca40824157a5bbcf102d7d86e569dbdb8439e7f47d694`
- Alpha-Testers SHA256 `639ce15ddb4b02d037cd29f995c9f38514e03cf577370cf38488f4602bfd65bd`
- Verification receipt SHA256 `e74d079b9b4df2d1d9778d2e15c4eebf72ee121449a38b525c1ceaf36aeab9a1`

TEST330 remains a published fallback. TEST345 must not be named current, verified, delivered or playable. No physical-phone testing occurred.

The executable build request blob is `3fe0ce4437fb3129a69588b2f943b1cc368c691d`; it was changed once for the intentional TEST345 candidate and was not edited for either verification-only retest or this handoff.

## Next safe step

The Privacy restoration package itself has exact-APK API34 acceptance. Keep TEST345 rejected overall and investigate the retained 276 ms warm-Home timing miss as a bounded performance package using the existing artifact and logs first. Do not rerun unchanged for green hunting and do not request another APK until a real application correction, if any, is ready. Preserve TEST341 as current and TEST330 as fallback.
