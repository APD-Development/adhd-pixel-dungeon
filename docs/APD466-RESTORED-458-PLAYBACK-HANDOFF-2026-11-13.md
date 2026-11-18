# APD466 owner-requested rollback to APD458 visual playback
November 13, 2026, Eastern time

## Explicit owner instruction
Owner strongly rejected the broad APD464/465 personal animation-speed overrides and instructed: **"go back to like build 458 or whatever and change only those 5 things or however many there were."** This supersedes the previous 58-category personal playback implementation. Do NOT treat the rejected APD464/465 playback system as the desired direction or silently restore its UI/combat/camera/idle overrides.

The exact previously requested 1x exclusions are: (1) floating status/symbol visuals; (2) enemy notice !/? icons; (3) Well of Health particles including one-shot use effects; (4) Well of Awareness visuals; (5) toxic-gas/gas-room particles. The three native 1x groups `statuses`, `emoicons`, and `gases`, plus `wellEmitter` routing for individual Health-well use effects, implement those exclusions. Everything else retains APD458's original selected game-speed behavior; no extra individual animation rules, no sound-speed modifications, no turn, hunger, scoring or buff-duration changes. Do not create a community preferences menu for the rejected 58-item matrix.

## Exact source and code
The original published APD458 source target is `8c74997210585191d12603ad5e2ee398b1c54fe7`.
Isolated rollback branch `chatgpt/apd458-original-speed-five-exceptions-20261113`, frozen source and producing workflow head `d1b3681d3a2a4e6e62c7cd9398f732843c039de3`.
Compared to 458 the only gameplay-source changes are:
- `PlaybackGroup.java`: add 1x normalSpeedGroup helper.
- `GameScene.java`: route statuses, enemy alert icons, gases to normal group; add `wellEmitter()` using gas group.
- `CellEmitter.java`: add well-only emitter.
- `WaterOfHealth.java`: route 4 one-shot activation positions and healing particle to well-only emitter.
- `PlaybackSpeedTest.java`: add all-ten-speeds isolation and well-clock tests.
Workflow/postcheck/this document are operational. Everything else, including sprite, UI, combat effects, camera, inventory, existing Search, saves, scoring, original APD458 design, is unchanged from 458.

## Verification and APK delivery
Original frozen paired build [run 43426522500](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43426522500) passed 3,266 core JUnit tests across 470 suites, zero failures/errors/skips, plus desktop and Android debug/release/androidTest Java compilation. Original-signer candidate matched certificate SHA-256 `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e`, package `com.charltonchris29.adhdpixeldungeon.indev`, `versionCode 920`. Frozen original candidate artifact ID `14036793380`, URL https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43426522500/artifacts/14036793380 .

Five of six native Android tests passed the original run; Developer WallGeometry's instrumentation passed `OK (1 test)` but its emulator disappeared during screenshot manifest transfer, so it was not accepted then. First recovery [run 43427833925](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43427833925) reproduced the post-instrumentation emulator disconnect, still not accepted. Second recovery [run 43428554135](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43428554135) on a fresh 3GB KVM emulator recovered that exact Developer WallGeometry case **without rebuilding the source or APKs**. Its native job passed, and the independent publisher verified exactly **six accepted frozen APK Android visual screenshot receipts** for original source and hashes, covering both variants and all three cases.

The subsequent GitHub Release API `gh release create apd-466` call still failed HTTP 403 `Resource not accessible by integration`. APD466 **does NOT have a successfully published GitHub Release**. Do not imply otherwise. The already-qualified Developer and Alpha APKs remain available inside the frozen GitHub Actions artifact and were downloaded into the originating conversation, extracted byte for byte and verified against the original receipt:
- `APD-Developer-466.apk`, 39,211,120 bytes, SHA-256 `d4aa46c75f7f89e620d6f308560e8ccd72b7707c123a372d64b1a62665a97c97`.
- `APD-Alpha-Testers-466.apk`, 39,211,092 bytes, SHA-256 `326c05d1619f2fcadd44264829c01558b7795939b2a07abd1701012d8abec6aa`.
- Matching original source ZIP is inside frozen candidate, SHA-256 `f7a3ea80ac57e145d565c2bba183784ea264b05515107c47c1876f4818f29d7d`.

## Constraints for continuation
User specifically wants the 458 gameplay/animation feel rather than 464/465 experimental per-category changes. Do not pull in those source changes. Both Developer and Alpha-Testers editions remain required. Preserve past releases and don't force-write shared branch. Original certificate, package and version code are unchanged. Rolling back to historical APD458 source may omit unrelated bug fixes added after 458; only owner-authorized narrow visual change was made. Installed-save roundtrip and actual physical RedMagic subjective speed acceptance have NOT been performed, so do not claim device proof. Release publication remains blocked by workflow token permissions, not by failing source/native tests; if permissions become available, publish **both exact frozen APKs and matching source**, verify release assets by hash, do not rebuild simply to publish.
