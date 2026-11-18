# TEST267 UIMSD legacy-date validation correction — 2026-10-17

## Result

TEST267 is a verified and independently staged private candidate at exact workflow source `c761afc25902954060daf5fffbd87e728b8b1887`, containing application correction `d76178a015b68344adfbeab2edfd701201c752a7`. Full run [39014635516](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39014635516) succeeded from 2026-10-17T07:42:30Z through 07:57:47Z. Independent staging run [39016526869](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39016526869) succeeded without rebuilding the application.

## Confirmed defect and correction

Complete review of `RowBuilders.java` found that `parseDate` accepted impossible legacy calendar dates because Java Calendar normalized them leniently. For example, 2025-02-29 or 2024-04-31 could become a later valid date and falsely populate `run.startMillis`.

Retained method `legacyDatesRejectImpossibleCalendarDaysButKeepRealLeapDays` first reproduced the defect. Run [39014149854](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39014149854) executed 1,774 methods and failed exactly that new regression. Artifact `12093313933`, 832,953 bytes, digest `sha256:ad6ac3f02ff85ecdb0b630004f1ba6b44433c85e264dd60f4e844e2e1c6881c9`.

Application commit `d76178a015b68344adfbeab2edfd701201c752a7` now validates the Gregorian day against the selected month and leap year before constructing the timestamp. Impossible dates return unknown; valid 2024-02-29 and 2000-02-29 remain valid. Raw history is not rewritten.

Corrected core-only run [39014400159](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39014400159) passed all 1,774 methods. Artifact `12094346515`, 830,170 bytes, digest `sha256:d3f775a284752db59b2118cabbd23cbe7686e24bc41d67f268fd3ac17a2d5dcc`.

## Full verification

- Core artifact `12094058905`, digest `sha256:e61492c44e1f7f4f2f710a0a5dddcb5a20c0b70081b137cae17c51d4b09af627`: 1,774 tests in 350 suites, 0 failures, 0 errors, 0 skipped.
- Android artifact `12093634842`, digest `sha256:6725223165d3f7f392dd361655fc4216b03df16e49eb9b5fab53f3150befc532`: 38 methods in 14 suites, 0 failures, 0 errors, 0 skipped.
- Debug assembly and strict immediate-previous compiled TEST266 history passed.
- All 57 required PNGs decoded as 320×640 and were manually reviewed. No blank or corrupt image and no obvious clipping or overlap regression was found.
- APK artifact `12093874225`, digest `sha256:f5ccfb2f66dfcc52eff2a13095711b4ba34102132b59750d7f18a291a53ad654`, contains `ADHD-Pixel-Dungeon-CODEX-TEST-267.apk`; raw SHA-256 `f5fc7189d981709cf46e740d1966814c64b5ed29a7e32bca921739ce4e04009f`.

## Retained supplemental display result

Optional exact-APK display run [39014635513](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39014635513) remains failed after three attempts. Small, square, M53 and compact profiles passed across retained attempts. Short-landscape reached the title after Changes but twice failed the Hide UI assertion at `TitleMenuLayoutDeviceTest.java:83`; the first attempt failed earlier waiting for the title. Failure logs and non-PNG placeholders remain retained. This optional result is separate from the required 57-image gate and is not relabeled green.

## Independent staging

Independent staging run [39016526869](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39016526869) succeeded from 2026-10-17T08:13:02Z through 08:13:43Z at wrapper source `87ce1ac4b10f27a2ca07db0529debd1139e38d0c`. It reused the exact tested APK without rebuilding.

Staged preview artifact `12094921683` is 44,588,415 bytes with digest `sha256:58a3e9e7e14ec1493038787b68a3674edf3db2b28aa28d5fbfb8367a95c399a6`; staged evidence artifact `12095106256` is 8,485,020 bytes with digest `sha256:df3021f1e8cd3fae7268e15ed5ac90b8421b0c20056b37784d4f69c29560048b`.

The staging gate preserved raw APK SHA-256 `f5fc7189d981709cf46e740d1966814c64b5ed29a7e32bca921739ce4e04009f`, verified APK signatures, application ID `com.charltonchris29.adhdpixeldungeon.indev`, version `4.0.8-apd-INDEV-bC267-TEST`, certificate SHA-256 `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e`, install-over version compatibility with TEST262, the private Cleric-lore feature marker and all 57 required screenshots.

## Preserved behavior and limits

Raw history, saves, all 18 enabled Custom Dungeon Rules, active 357-value v2.6R scoring, all lookup values, original scores, gameplay, turns, RNG, Cleric lore, private Grudge enablement, opt-out and collection ledger are unchanged.

No physical-phone or owner-save install-over test is claimed. No tester distribution or publication occurred. TEST267 is staged only. TEST266 remains a preserved staged fallback and TEST262 remains the latest actually published owner APK. The broader UIMSD audit and gameplay backlog remain open.
