# TEST179 — UIMSD column-dialog recreation regression

Started: 2026-10-14T23:39:51Z

## Reviewed issue
The source guide still identified Arrange columns as disappearing when Android rebuilds UIMSD. Current production code now assigns exact restorers to both the Column order list and its Move column child, and PixelScene captures those restorers during scene reset. The retained API34 suite covered Columns selection but did not directly recreate either step of this two-window workflow.

## Bounded regression
The existing `menusSurviveSceneRecreationAndNarrowActionsStayInsideTheirWindow` device test now:
1. opens Column order with a multi-column query;
2. rebuilds the UIMSD scene and requires exactly one restored Column order window;
3. selects the second column, rebuilds again, and requires exactly one restored Move column window;
4. invokes Make first through the restored callback, requires return to Column order, and verifies the chosen column actually became first;
5. restores the original query columns before continuing the existing test.

Production query/window code, player history, saves, scoring, application identity and signing are unchanged.

## Verification request
TEST179 requests the unchanged 1,602 core tests, debug assembly, compiled history against immediate previous TEST178, all 26 API34 methods with the strengthened recreation assertions, and 34 valid screenshots. Candidate remains pending until exact-source evidence passes.

Private delivery is not requested. TEST123 remains the last owner-delivered/signature-compared APK.


## TEST179 exact-source result — fixture failure
Exact source `f6043c7d16cd52e57276113fb038af046ff4ec48`, run `38655567956` (2026-10-14T23:40:55Z–23:52:01Z), concluded failure after the complete device suite. The unchanged 1,602 core tests, debug assembly, immediate-previous TEST178 history validation and private APK packaging passed. API34 ran all 26 methods: 25 passed and this new method failed at `column-order fixture has at least two columns`.

The production Runs query validly represents its default columns with an empty explicit list. The new fixture saved that empty list, restored it after its single-column check, then incorrectly treated it as a literal zero-column layout. This is a test setup defect, not evidence of a production lifecycle failure. Android artifact `11937227267` (ZIP SHA-256 `5e80aad0730b6507fe0cfba1862fae175636d70362eeac2c8f25f5508718a3e8`) retains the exact failure; core artifact `11937375321`; APK artifact `11936159775`. TEST179 is not verified and was not delivered.

## TEST180 fixture correction request
Resolve the effective test layout from dataset defaults when the original query's explicit column list is empty. Exercise the two recreation steps against that effective multi-column list, then restore the original explicit list (including empty/default semantics). Production code remains unchanged.

TEST180 requests 1,602 core tests, debug assembly, compiled history against the immediate previous compiled TEST179 APK, all 26 API34 methods, and 34 screenshots. Candidate remains pending until exact-source evidence passes. TEST123 remains the last owner-delivered/signature-compared APK.


## TEST180 verified
Exact source `acf8592e2db3c30684e09a05bd22c097c0d5f077`, run `38656873159` (2026-10-14T23:54:11Z–2026-10-15T00:06:25Z), concluded success.

- Core artifact `11937556865`, ZIP SHA-256 `3e33f34ed08a3905d67d97652a25fa627dff485aa08134686af4afad8fa1fa2b`: 1,602 tests, 0 failures and 0 errors.
- APK artifact `11937113364`, ZIP SHA-256 `2445194a97a19a118ff453f214a849dd24488fcddd529e5e0d0743a05383dde7`: `ADHD-Pixel-Dungeon-CODEX-TEST-180.apk`, SHA-256 `279f6691874d32a892fc2743afe5946b3bdf371f315372b7541ffec646ab8332`; bundled checksum matched. Debug assembly and immediate-previous compiled TEST179 history passed.
- Android artifact `11937448955`, ZIP SHA-256 `a9638818a3f7afacbeaa05596fc1fb3b8875e3c06d1320220e2da1f64dff46b9`: 26 API34 methods, 0 failures/errors/skips. The strengthened Column order/Move column method passed in 8.33 seconds.
- All 34 PNGs decoded at 320×640. Contact-sheet and focused APD Changes review found the expected TEST180/TEST179/TEST178 sequence and no blank, corrupt or visibly clipped capture.

This verifies the retained API34 regression and the fixture correction; production query/window code was not changed. Private delivery was skipped. This is CI/emulator evidence, not a physical-phone claim. TEST123 remains the last owner-delivered/signature-compared APK.
