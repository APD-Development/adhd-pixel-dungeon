# APD TEST24 Community private test report — October 10, 2026

## Delivered source and verification

APK candidate source is real commit f1ad8657250defd4b97cf422fa222d6c6c1f7329 on codex/apd-phone-fixes-2026-10-09. Build run 38075683960 builds this exact source, not the local synthetic Git snapshot. The attached automatically generated verification section contains final successful test counts, source, checksum, signature comparison, application ID and timestamps. Private publication refuses a failed or incomplete run, missing test evidence, skipped tests, duplicate evidence, checksum/signature mismatch or application-ID/version incompatibility. Existing releases and player data are preserved.

The complete core suite passed: 947 tests in 151 suites, zero failures/errors/skips; debug assembly succeeded. Downloaded API 34 XML confirms all six Android checks passed, including both new actual-touch checks, with zero failures/errors/skips. Core XML execution totaled 46.782 seconds; Android cases totaled 241.867 seconds. Publication additionally requires direct signing checks. Emulator evidence is not physical-phone verification.

## What changed since TEST22

Community is now accessible from its own main-menu button, separate from UIMSD and its ten-run unlock. The APD Settings row is split between Settings and Community, retaining the existing menu height and other navigation. Previously, Community existed only in an unfinished local checkpoint and had not been committed or delivered; this was incomplete implementation/delivery, not evidence of a working feature.

The hub opens eight readable pages: submissions, qualification voting, tournament brackets, community-selected content, run reviews/challenges, mods/competitions, events, and development/testing. Each page describes its purpose and availability. Month 1 is submissions and discussion; Month 2 chooses the top 16; Month 3 is the final-16 weekly elimination tournament. Discussions have dedicated threaded comments/replies with author participation and remain available during future voting; comments do not count as votes. Submission and comment placeholders contain no fake user content or example pitches. No clans or internal tester dates appear. Online operations are explicitly planned/unavailable; no backend is activated. Cleric restrictions and creator-controlled content are unchanged.

Adult humor now follows the owner's clarified policy. With no age provider, the switch is usable and defaults to Clean; Adult/Clean preferences persist. The policy defaults verified adults to Adult unless they chose Clean, and blocks verified underage players. Previously, unavailable verification blocked the switch entirely. The application currently has NO connected age-verification provider; pure policy tests do not establish verified age or legal coverage. Underage handling requires a future real provider. Pending UIMSD title wording uses the actual saved humor preference. Progression and prompt-retirement rules are unchanged.

The ten-run instructional message remains above the UIMSD button while the gate is locked. The drug/dopamine arrow is the pending tenth-genuine-run prompt and retires after opening UIMSD; demo unlocking does not produce that genuine-run prompt. The owner's screenshot alone does not expose saved unlock flags. This update does not promise an always-visible arrow or silently alter the ten-run rule.

## Root cause of the failed intermediate Android check

Intermediate humor candidate 0c35d1bc402a9968d07f007e29db501e1a056b46 / run 38074287348 passed 943 core tests and debug assembly, but only 4 of 5 Android checks passed. The new humor test timed out before touching settings because it assumed Activity launch resets to TitleScene. The game constructor deliberately restores the previous static scene; logcat showed the preceding tests leaving gameplay/hero-selection screens. Test setup now waits for an initialized game and explicitly navigates to the title. Application startup behavior was not changed to make this test pass. The failed candidate was not published as verified.

## Tests and their limits

Four Community JVM tests cover eight destinations, required pipeline/comment semantics, no clans/internal dates, explicit offline availability and no unlock/queue/network dependency. Frozen-title and UI-entry guards retain protection while recognizing only the approved Community edits. Fourteen targeted Community/navigation tests passed locally in 0.087 seconds.

The new Android Community regression taps the actual main-menu button, opens all eight pages, checks their content and availability, verifies a single dialog rather than stacking windows, checks horizontal text bounds, returns through every Back button, and returns to the main menu. The humor regression taps Adult and Clean, reopens settings, and checks the saved controls. Existing checks exercise UIMSD recreation/narrow actions, gameplay/save/continue/controlled death, controlled victory recording, and hero difficulty/gear navigation. Final actual XML is required before publication. These are six targeted checks, not the entire instrumentation suite, a complete normal 25-floor run or every aspect ratio.

Seven delivery-tool unit tests pass locally. New refusal tests reject absent, failed, errored, skipped, incomplete and duplicated XML evidence. The verifier also checks actual case counts and requested new Android method names. Existing signer-format tests retain support for numbered and SDK V2/V3 certificate labels and reject malformed/source-stamp signatures. Delivery reuses the already assembled APK; no redundant Gradle build is required.

Five new local normal-loot regressions passed in 0.579 seconds: real Healing/Shielding identification for Swarm/Bat/Necromancer, the existing shared potion-appearance mapping, ambiguous/unrelated item non-identification, unchanged RNG draw/loot chance/limited-drop counters, and normal-drop hook placement separated from bonus loot. This remains local work, NOT included in TEST24. Direct complete drop/bonus placement integration and its own full build checks remain pending.

Five local movement regressions passed in 0.044 seconds: expected 0.75x/1x/2x duration, actual PosTweener wall-clock timing at 30/60/90/120/144 Hz, saved preference reopening, invalid-value fallback, and unchanged actor time/global clock/RNG draws. This remains local work, NOT included in TEST24. APD's current 0.1-second movement interval matches inspected upstream SPD; the owner's reported fast animation has not been reproduced or assigned a confirmed root cause. No physical-phone speed fix is claimed.

Simulator framework commit 38f761c067190345b66e307628799f22938a6dcc has eight passing fixture regressions locally and in CI 38073970360. Native libraries and real PNG assets recovered through approved CI allowed six existing actual first-floor generation tests to pass locally (1.390 seconds). Initial local failures were missing assets/stale compiled harness, not confirmed game bugs. No real 100-run Warrior experiment has completed. Fixture outcomes are not survival/balance evidence.

## Relevant files and commits

Humor application: core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/age/AgeEligibility.java, SPDSettings.java, windows/APDSettingsPane.java. Regressions: apd/age/AgeEligibilityTest.java, windows/UimsdGateTest.java, windows/UimsdSettingsPersistenceTest.java. Android source: android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/ApdSettingsClickDeviceTest.java.

Community application: core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/community/CommunityPreview.java, scenes/CommunityScene.java, scenes/TitleScene.java. JVM regressions: apd/community/CommunityPreviewTest.java, apd/UimsdWiringTest.java, apd/UiEntryPointsTest.java. Other verification/report files: .github/workflows/codex-sprint-check.yml, docs/APD-CODEX-BUILD-REQUEST.txt, docs/APD-COMMUNITY-ALPHA-DESIGN.md, docs/APD-WORKFLOW-AUDIT-2026-10-10.md, docs/HANDOFF.md. Relative test paths are under core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/.

Exact real commits: simulator framework 38f761c067190345b66e307628799f22938a6dcc; humor correction 0c35d1bc402a9968d07f007e29db501e1a056b46; native asset retention c582abcec0c71b1e3a3c28e57a84db612c498459; Community/current APK source f1ad8657250defd4b97cf422fa222d6c6c1f7329. Delivery-only tooling/report has separate later history and does not change APK source. Main and Claude's work are preserved; no force-push or synthetic local history is published.

## Potential regressions and phone testing

The split Settings/Community row changes two button widths; test their touch targets and text on the RedMagic and other screen sizes. Test every Community page, scrolling, Android Back, and returning from another app. The previews do not submit data or vote. Future rules marked undecided are not finalized by the UI.

With verification unavailable, existing saved Adult preferences become effective under the owner's revised policy; confirm the preferred Clean/Adult setting on upgrade. Verify UIMSD counts/history, saved views, dialogs and the expected progression message with the actual phone's stored state. Install over the matching indev application. Never uninstall or clear data. Actual upgrade/save preservation on the owner's phone is still unverified. No new game-data schema or gameplay-turn/balance change is introduced in TEST24.

TEST22's queue corruption/duplicate-ID protections, Blacksmith restoration fixes and diagnostic wording remain included. Damaged queues intentionally refuse add/remove/send to protect original bytes; no destructive repair is supplied. Long normal-run completion, all quests/bosses, process death and actual old-save upgrades still require additional coverage. No evidence justifies claiming APD or UIMSD contains zero bugs.

## Unattended execution and honest timing

Existing hourly automation 6aca774b2e2081918d6025f01eb471c5 was updated with bounded real execution, safe Git leases and a separate actual scheduler/GitHub probe. Its immediate-run action returned HTTP 404 Action not found before invocation. At the last inspection its last/next run timestamps were absent; no probe commit was observed. UNATTENDED DEVELOPMENT HAS NOT BEEN DEMONSTRATED. The enabled record is not proof of execution.

The local CLI is logged in through ChatGPT, not an API key, but this workspace is ephemeral and has no persistent app-server/scheduler or configured CLI GitHub MCP connection. Web scheduled tasks do not preserve this local folder between runs. The simplest documented persistent local option is desktop automation on an awake PC with the app open and APD checked out, using subscription authentication; that PC setup has not been installed or demonstrated from this session. No paid API dependency, new billing or fake automation was introduced. Official references: https://learn.chatgpt.com/docs/automations?surface=app and https://learn.chatgpt.com/docs/auth .

The prior roughly 30-hour audit found 42 real repository commits; aggregate diff changed 43 production Java files (468 additions/205 deletions) and 29 Java test files (1630 additions/42 deletions). These include other developers, documentation and delivery; they are not measured Codex hours. Complete active coding time, idle totals and longest inactivity cannot be reconstructed. Prior observed GitHub write waits were 385.6 and 524.6 seconds before user cancellation; they do not explain every overnight hour.

TEST22 was published at 17:46:56 UTC / 1:46:56 PM EDT, after the required lunch deadlines. Current Community source checkpoint was recorded at 18:24:47 UTC / 2:24:47 PM EDT. Final build timestamps are in the generated verification evidence. CI elapsed time is not active coding time. No uninterrupted work while a chat is closed is claimed.

## Unfinished approved work

The actual 100-run simulator adapter/results remain unfinished. Category/custom hotbar and Cleric one-to-four quick-cast controls remain partial, with overlap/touch/compatibility checks required. Healing confirmation, guaranteed enemy drop identification, movement-speed settings, compact weapon accuracy information and Crystal Path guaranteed final rewards remain separate local work needing completion and build/device verification. No alternative starting Warrior potion or other speculative balance change is made. Community is an informational preview, not operational online voting/comments/mod downloads/cloud accounts.

No hidden achievement, Cleric restriction changes, monetization, paid advantage, live backend, public release or unrelated gameplay feature is enabled.

## Verified candidate checkpoint — 18:36 UTC

Run 38075683960 completed successfully. Actual Android method evidence includes communityMenuOpensEveryPreviewAndReturnsWithoutStackingWindows (17.431 seconds) and adultHumorControlWorksWithoutAnAgeProviderAndReopensWithSavedChoice (6.787 seconds). Both opened real controls through pointer events. Signing, package/upgrade version and final artifact checksum are checked by the delivery-only workflow before this report is published. Existing TEST22 is retained as rollback.

## Delivery-only upload failure and recovery

Delivery run 38076542746 successfully checked the exact source, 947/6 tests, checksum, signatures, app ID and version compatibility, and generated/staged the APK/PDF. Its release step then returned HTTP403 Resource not accessible by integration while targeting the earlier source commit. No incompatible or failed build is involved. The recovery uses the current delivery commit as the release-tag target and additionally checks that every application/build root entry matches f1ad865 exactly; only docs/tools/workflows may differ. This preserves actual APK source evidence and does not request broader token permissions, change repository permissions or bypass security. Whether this resolves the rejection is determined by the recovery run; a second failure remains a delivery blocker, with the verified artifact preserved.

Verified APK SHA-256: e13408155a0534726686dd36f45c2ea377bb242dd02708891a234599f8b50785. App ID com.charltonchris29.adhdpixeldungeon.indev; version 4.0.8-apd-INDEV-bC24-TEST. Direct certificate comparison with TEST22 succeeded: 067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e. This does not prove the owner's actual old saves survived a phone installation.

Two additional source-provenance regressions pass: delivery documentation/tooling may differ while app/build trees remain identical; changes to source, Android/assets or build configuration cannot pass. Total current delivery-tool tests: nine, zero failures (0.004 seconds locally).

## Updated unattended evidence — actual scheduled probe

After the initial 404 and missing-run observation, the hourly scheduled task DID execute. It read real GitHub state and safely added docs/APD-UNATTENDED-PROBE-2026-10-10.json in real commit 7ead4a6e4891b78314e2f05ed69c15f889260a43 at 18:39:32 UTC. Recorded execution began 18:38:07.955 UTC and repository inspection ended 18:39:23.241 UTC (75.286 seconds elapsed inspection, not measured active coding). The fresh workspace was empty with git/java but no checkout/gradle/adb. This confirms ONE scheduled GitHub read/write execution using available tools. It does NOT establish unattended application coding/builds, future successful iterations or 15-hour continuous activity. The earlier statements of no demonstrated run are checkpoint history, superseded by this limited evidence. The concurrent probe commit is preserved and recovery uses it as the real parent. No API key or added spending was introduced.

## Publication confirmed

## TEST24 delivered with Community — 2026-10-10 18:46:52 UTC

Private release apd-codex-test-24 published 2026-10-10T18:42:59Z; recovery run38076840351 SUCCESS. Actual APK SHA256 e13408155a0534726686dd36f45c2ea377bb242dd02708891a234599f8b50785, 38,225,942 bytes, same signing certificate as TEST22, indev application ID unchanged, versionC24, nondowngrade checked. Actual build source f1ad8657250defd4b97cf422fa222d6c6c1f7329; release tag points fb1a728b852d5c722ff9ae16676b727143e0904a (delivery tooling). All application/build root tree entries were compared and are identical to tested source; evidence JSON records both commits. No APK rebuild or permission changes. First release targeting historical source returned403; current delivery-commit target succeeded with same token. Exact internal GitHub reason for initial403 remains unknown.

Actual 947 core tests +6 API34 checks passed. Community main-menu entry, all eight pages, back navigation and text/window containment verified through actual pointer test. Adult/Clean persistence verified through actual pointer test. APK/PDF delivered in chat. Physical phone upgrade/full-run checks pending. Download APK: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-codex-test-24/ADHD-Pixel-Dungeon-CODEX-TEST-24.apk
PDF: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-codex-test-24/APD-Bug-Fixes-2026-10-10-TEST24.pdf
Evidence: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-codex-test-24/TEST24-Verification.json
TEST22 remains rollback; NEVER uninstall/clear player data.

One hourly scheduled GitHub read-write probe completed at7ead4a6; no unattended application coding/builds or overnight continuous execution proven. Latest new loot/movement regressions remain local and outside TEST24; no device refresh-rate changes. Actual100Warrior simulator still unfinished. Next independent approved work continues while active execution exists.
