# TEST362 exact occurrence-to-timeline native acceptance — 2026-11-05

## Outcome

The documented source-only acceptance gap for the UIMSD **Run history at this turn** bridge is closed on the exact already-published TEST362 Developer APK. No application source, APK, version, release, signing identity, application ID, save/statistic format, gameplay, RNG, all18 rule values or v2.6R scoring input changed.

## Immutable application identity

- Application source: `34f5ffd2ea08bef701f60c95298edcfacc239e0c`
- Producing run: `41942317979`
- Developer APK SHA-256: `1085bb39899a9c3c1ff5a9dd75ccaa0a73f05833aac8340b963bfdac16609f5a`
- Test source: `59aeaf14e81725803725473cfd2c71f615a25980`
- Application rebuilt: **false**
- Application inputs unchanged: **true**
- Current private release: TEST362 / `apd-alpha-preview-362`
- Executable build-request blob: `bed60205ff0892d7890c61a6c0c5848f4e844868` (not edited)

## Added native acceptance

The added API34 method `exactOccurrenceRowOpensRecordedTurnAndRestoresReadOnlyTimeline`:

1. writes and closes a real binary run log;
2. loads it through `DatasetStore`;
3. opens the exact recorded damage occurrence;
4. touches **Run history at this turn** through the rendered row action;
5. proves the canonical browser opens on turn 21 of 30 with recorded evidence;
6. restores the timeline after scene reset at the same exact turn and content;
7. captures `uimsd-occurrence-history.png`; and
8. proves the persisted raw log bytes are unchanged.

The prior 71 cases and their evidence requirements remain intact. The screenshot was added to both extraction and visual-validation allowlists.

## Terminal evidence

- Workflow: [42051261144](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/42051261144)
- Job: `127327661933`
- Completion: `2026-11-05T15:17:02Z`
- Instrumentation: `OK (72 tests)`
- Runtime reported by AndroidJUnitRunner: `556.121` seconds
- Required PNGs: **107**, all validated at **320x640**
- Evidence artifact: `13427813430`
- Artifact size: **7,935,252 bytes**
- Artifact digest: `sha256:0a06d470b2da5fac13db5248840749ab6cbb52104ae7baf8ca8e03effbf86fa1`
- Evidence archive files: **110**

Manual visual review of the new capture confirms the title **Run history - turn by turn**, **Turn 21 / 30**, recorded floor-2 hero damage caused by rat for 7, and visible/unclipped Previous, Next, Step and Go to turn controls.

## Retained unrelated failure

Because the older `.github/workflows/apd-existing345-full-api34.yml` intentionally watches the shared UIMSD native test file, run `42051261345` also started. It failed closed at the old TEST345/current application-input equality assertion, then skipped compilation and emulator execution. That old APK predates substantial application changes; the failure is retained and is not evidence against TEST362. It assembled and published nothing.

## Boundaries and next work

No physical-device claim is made. TEST362 remains the current actual private release and TEST358 remains preserved as fallback. The unconfirmed TEST254 fire report and broader representative Android UIMSD latency work remain open. Continue UIMSD reliability/audit source-first and reserve another candidate for an actual application-source change; do not rebuild TEST362 merely to restate this evidence.
