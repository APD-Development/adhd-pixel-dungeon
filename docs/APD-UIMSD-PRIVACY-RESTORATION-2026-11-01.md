# APD UIMSD Privacy restoration — 2026-11-01

## Disposition

Core-green source change only. No APK was requested, built, named, published or distributed by this package. TEST341 remains the current private published pair; TEST344 remains rejected.

## Reconciliation

- Starting approved-branch head: `377c3e20b7dc6ec6c4d149c5c1b4ea7cda3bb7ed`.
- Queued/running workflows before mutation:0/0.
- Build-request blob before and after: `4f73694d9a0a9e6d6e4d4ca9ccffc71e94a07709` (unchanged).
- Latest APK publication: TEST341 release428527455/tag `apd-alpha-preview-341`, originally published2026-10-30T00:24:54Z.
- Release metadata updated2026-11-01T03:49:33Z to add matching-source archives and compliance disclosure. APK bytes are unchanged:
  - Developer SHA256 `ad6281b3e41fe414187ca40824157a5bbcf102d7d86e569dbdb8439e7f47d694`.
  - Alpha-Testers SHA256 `639ce15ddb4b02d037cd29f995c9f38514e03cf577370cf38488f4602bfd65bd`.
  - Verification receipt SHA256 `e74d079b9b4df2d1d9778d2e15c4eebf72ee121449a38b525c1ceaf36aeab9a1`.
- TEST341 still contains the older restricted JSON dependency. Do not call it fully GPL-compliant; make its matching source archive available with any APK relay.

## Confirmed defect

The parameterized-window scan found no missing `WndUimsdList` restorer outside one coherent Privacy cluster in `WndUimsdOptions`:

1. Evidence-request list.
2. Evidence-request decision.
3. Community-vote offer list.
4. Checked vote in progress.
5. Account export/deletion confirmation.

These windows have no zero-argument construction path. Without an explicit restorer, `PixelScene.saveWindows` drops them on scene recreation. The checked-vote case also needs semantic state preservation; simply reopening from storage would lose the user's current ticks.

## Correction

Production commit `c501320ccdcbeb15659f7de228771180779cfcae`:

- captures exact local evidence entries and vote offers for restoration;
- restores the exact evidence entry and account-request kind;
- restores an in-progress vote with a defensive copy of its currently checked option set;
- performs no vote cast, account/evidence action, queue mutation, send or connection during restoration.

The change does not activate online functionality and does not alter saves, permanent history/statistics, gameplay, RNG, all18 Custom Dungeon Rules, nine challenges, difficulty, provisional v2.6R scoring, original scores, signing, application ID or version.

## Regression evidence

Focused method: `UimsdWiringTest.privacySubflowsRestoreTheirExactLocalStateAcrossSceneResets`.

Core request head: `2a8d3e4139bef18b602e37a7fae2457d66a0ecc1`.

GitHub Actions:

- Run41319425592, job124886277582.
- Started2026-11-01T15:05:29Z; completed2026-11-01T15:07:34Z.
- 425 suites.
- 2,232 tests.
- 0 failures,0 errors,0 skipped.
- All6 `RunTurnWindowTest` methods passed.
- Artifact13089090788,1,046,318bytes.
- Artifact digest `sha256:d88d08608b9de5bfd81f64c2d888dc0afb72ad73ecbb0b8149ad27e38930a4f3`.

## Limits and next safe step

This run supplies no Android, display, APK, physical-phone or publication evidence. The next bounded package is one API34 scene-recreation case using local synthetic evidence/vote state, proving exact list/confirmation/tick restoration and proving restoration itself causes no queue/send/connect action. Do not request a new application candidate until that acceptance is ready. Do not rerun rejected TEST342/344 unchanged for green hunting.
