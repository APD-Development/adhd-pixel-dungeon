# APD TEST344 UIMSD Search Restoration Verification Report

**Recorded:** 2026-10-31T19:37:22Z  
**Approved branch:** `codex/apd-phone-fixes-2026-10-09`  
**Candidate head:** `d63962ba6a814584aadb91bd65bff5f2dfeb17ba`  
**Application change:** `0298c2f8f3704d0b0ba47b8ae2975ace2ef2152a`  
**Disposition:** rejected; private workflow artifacts only; not published or distributed

## Reproduced failure and correction

`WndUimsdSearch.results` created a parameterized `WndUimsdList` without an exact scene-reset restorer, so `PixelScene.saveWindows` discarded the open results list. Its result opener also captured the destroyed `UimsdScene`. The correction restores the exact query/catalog and routes a restored selection only to the rebuilt active UIMSD scene.

The Android acceptance opens a real Search prompt, captures the ordered results and active query, rebuilds the scene twice, requires one exact-restorable results list after each rebuild, verifies the query/order are unchanged, and proves a restored result selection dirties the rebuilt scene. It changes no save, history, score, rule, combat, RNG, content, signer, application ID or versionCode.

## Evidence

| Gate | Actual result |
|---|---|
| Core-only | Run41232001787/job124627987345:425 suites,2231 tests,0 failures/errors/skips; all6 RunTurnWindow methods |
| Candidate producer | Sprint344 run41233486143/job124632323324 passed history, tooling, core, debug assembly and compiled-history verification |
| Developer APK | SHA256 `3c47ff1acfd816c720d24f30d5d569c4b73c44fda68e9ac55985406955e911bd`; artifact13054557985 ZIP `0c0c20f1e9178335ee2d7896677210e0721fa497129a1a402926e616646e952b` |
| Matching player | Run41233486166 passed identity/signing; SHA256 `8c3fa76d68e7f7b712dab0c0d080095d52d28c586057712350c9be0aaa509175`; artifact13054503862 ZIP `44f923934b6277b2c7f31d961aa0f3477b95bbc3094e13b914716c432014dbb4` |
| API34 | Complete existing66-method suite passed, including Search restoration;92 PNG captures validated |
| API34 artifact |13054109759; ZIP `9f7c94ee47254aac8d8ddcd649b290693ad01d301dddba3f5cb35a24de1a005b` |
| Display passed | small, compact, square, M53 |
| Short-landscape | Failed after offline emulator startup; render timeout at TitleMenuLayoutDeviceTest line175 and non-PNG evidence; artifact13054452833 ZIP `a29f3a9effc901d21f1891fe25aa705f2c8071989279ac18f3f8a664bcd51128` |
| API36 | Broadcast-idle reached; first Changes-page Back failed title restoration at line150; later2 scale methods passed |
| API36 artifact |13054223697; ZIP `ee9203646e8477b860755e1a700dc64edaf7020e72a4d349fb4284023e96c916` |

Automatically triggered staging run41233486154 failed its publication-tree equality guard. Legacy hotbar run41233486149 and death-order run41233486080 failed before native execution because `git diff` could not resolve `eda307d567bd7eacd1b304042747b65d0ea4ed72`. These failures were retained and supply no acceptance evidence.

## Current APK status

The latest actually published private pair remains TEST341, release428527455/tag `apd-alpha-preview-341`, published2026-10-30T00:24:54Z:

- Developer SHA256 `ad6281b3e41fe414187ca40824157a5bbcf102d7d86e569dbdb8439e7f47d694`
- Alpha-Testers SHA256 `639ce15ddb4b02d037cd29f995c9f38514e03cf577370cf38488f4602bfd65bd`

TEST330 remains a published fallback. TEST344 must not be named current, verified, delivered or playable. No physical-phone testing occurred.

## Next safe step

Preserve the exact TEST344 artifacts. Do not rerun unchanged for green hunting or publish. Continue only with a bounded diagnosis of the retained short-landscape emulator/render failure or the already isolated API36 system-Back delivery failure, while keeping emulator and physical evidence separate.
