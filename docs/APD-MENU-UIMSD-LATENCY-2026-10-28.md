## 2026-10-28 — TEST320 menu/UIMSD latency: core, main Android and five displays passed; physical acceptance pending
- Exact compiled source `71901973f0fe1364690a4f9b41aba420d2b3e83f`, production repair `b1eba520`. Main producer/native run [40713885330](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/40713885330) passed all **2,110 core methods /409 suites** and **60/60 API34 methods**, zero failures/errors/skips. Reviewed82 native PNGs; artifact12829480910. Developer APK SHA-256 `bd3782190d25506543404780036986b8d323545318ae95e2782f13746c7c4800`, 38,783,840 bytes. Matching player run40713885318 passed2110 core and signer/application-ID/history gates; player SHA-256 `68a395ac0ee9a34e73e98bac33bf2abf48ed2205b2f7a1bf0a35147b457350dc`, 38,783,663 bytes. All346 TEST319 predecessor records retained;347 indexed records /348 TSV assets in both.
- Real Android-key Back plus render responsiveness round-trip: About80ms, Journal64, Top Scores82, Play slots90, Hero selection63, News63, Changes95, Community64, UIMSD68. All9 under250ms. The 72-run synthetic timing fixture prepared a READY UIMSD model in280ms and handed it over after metadata validation in7ms. **7ms is model handoff, not measured end-to-end scene opening**. Cold dataset construction4357ms; cached reload264ms; Home build35ms. No physical REDMAGIC latency claim.
- Exact-current-APK display run40713885373 passed all5 configurations, including320x480 and960x320. Reviewed20 images: one m53 options capture contains the preceding title frame and its restored image is compositor-transitional; native checks passed and main API34 captures show intended options/restored states. Footer test now waits for actual restore completion before the same outside-icon press; no touch assertion skipped. APD Settings implementation unchanged.
- Preserve real limitations: old hotbar restart run40713885434 and death-order run40713885361 fail BEFORE their tests because shallow checkout lacks pinned `eda307d5`; no supplemental cold-restart/death-order acceptance. Staging40713885439 failed closed before producer artifacts existed. Main/native and all5 display suites passed, but do not call every workflow green. No GitHub release or tester distribution. Both APKs prepared for owner download, physical install-over/save/timing acceptance pending. Full evidence in `docs/APD-TEST320-VERIFICATION-2026-10-28.json`. Do not rebuild unchanged source merely to add this evidence.

# Menu Back and UIMSD latency repair, pending Android acceptance

Base: `7e14d65217d331acfb07fd9ffe2ad1091654c2f8` on
`codex/apd-phone-fixes-2026-10-09`. Owner request: remove UIMSD lag and
Back-navigation lag throughout menus, excluding APD Settings.

## Changes

- UIMSD opening schedules recorder flushing, warm-cache freshness checks and
  loading on its background loader. The render thread no longer waits for the
  recorder's up-to-300-ms flush or scans every history file's metadata. A warm
  catalog is accepted only after the flush and the existing metadata check;
  cold loads retain the complete raw-history validation.
- The exact validated warm catalog, saved column layouts and prepared Home
  transfer to the opening model. A READY model builds its visible statistics
  during scene creation rather than first drawing an empty background.
- UIMSD Back retains exact query results for its four nearest destinations.
  Dataset identity guards reuse; older destinations recompute normally.
  Page changes clamp the page over the existing result without rerunning queries.
- Pending Android Back scene requests are applied before drawing, eliminating
  an unnecessary outgoing-menu draw. Ordinary scene updates retain their
  scheduling; requests made during creation remain pending for the next step.
- Title-arrow checks use the existing genuine-run unlock policy, which persists
  an earned unlock. Previously this title-only count check did not persist it,
  permitting repeated full database loads on subsequent menu returns.
- Android Back fixtures explicitly enter TitleScene so a previous test's UIMSD
  scene cannot cause a false startup timeout. The standard Android inventory now
  includes the previously omitted UimsdDeviceTimingTest.

APD Settings implementation, gameplay, saves, scoring, history records and the
18 Custom Dungeon Rules are unchanged. Menu scene reconstruction still exists;
this package removes identified blocking/repeated work and an extra draw, not
the scene lifecycle itself.

## Local verification

- Java 11-target compilation with Eclipse ECJ 3.40.0 against actual repository
  SPD-classes, top-level query classes, stats classes, DemoData and test sources.
  Dependencies: libGDX 1.14.0, controllers 2.2.4, FreeType 1.14.0, jnigen loader
  2.5.2, org.json 20240303, JUnit 4.13.2 and Hamcrest 1.3. No game/model stubs.
- 86 JUnit tests passed across NavigationViewportTest, UimsdWarmCacheTest,
  UimsdModelTest, QueryEngineTest, UimsdSafetyTest, SavedViewsTest,
  ColumnOrderTest and SceneRequestTest. The safety test compared 28,134 query
  results without a difference.
- The new exact-result Back and pagination assertions fail against the original
  UimsdModel, and pass against this repair. Other eight viewport checks pass
  against the baseline too.
- New tests cover worker-only flushing, reuse of actual warm catalog/Home,
  post-flush invalidation, obsolete-load rejection, bounded Back caching,
  dataset replacement and single-consumption scene requests.
- `bash -n tools/run-codex-android-checks.sh` passed.

The complete Gradle core gate subsequently passed at source
`288b6f59ae568ffbc263bd31be6931d092261c9e`, run `40712827076`, job
`122902408110`: 2,110 methods /409 suites, zero failures/errors/skips, with all
six RunTurnWindow cases retained. Artifact `12827556330` has ZIP SHA-256
`6b75b65643cd2da47554462e21754ad35e7034a1b158dc370e9bdb3a2d92f65a`.
No physical-phone speed or complete lag-resolution claim is made. TEST319
remains rejected. A subsequent native-only test adds actual Android Back timing
across nine menu routes with a 250ms responsive-title budget. The footer test now
waits until the restore tween re-enables Hide UI before retapping the exact same
outside-icon location, preserving its native hit and hide assertions.

## Next verification

Following the owner's reiterated request after the verification/build question,
continue with the full core suite and Android compilation/native checks on this exact source,
including actual UIMSD entry timing and Back across all menu destinations.
Retain prior failed footer-touch checks rather than masking or skipping them.
Inspect resulting timing evidence before deciding whether remaining menu scene
construction needs further optimization. Build an intentional matched private
APK pair only under the approved build process; preserve compiled history and
existing application/signing identity. Physical owner testing is still needed.
