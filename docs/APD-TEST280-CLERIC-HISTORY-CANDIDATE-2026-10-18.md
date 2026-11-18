# TEST280 Cleric history candidate

Recorded: 2026-10-18T14:01:17.290Z

- Application source: `64beb4867feb73eb03257f6cbe1a32648e84ae10`.
- Confirmed defect: `ClericHistory.compute` incremented the run total after recognizing a Cleric class but before requiring a recorded outcome, so an incomplete finished row was presented as a real historical run.
- Reproduction: core-only run `39133186514` executed 1,832 tests and failed only `finishedRowsWithoutARecordedOutcomeAreNotInventedAsRuns`; artifact `12139205223`, SHA-256 `685c3a5ea34f6a6146eff329d85f97a108eb4d8dc58f6c2e7e65b4e9d133d85c`.
- Correction: require both recorded class and outcome before counting. No lore text, valid win/death result, gameplay, save, raw history, score or RNG behavior changes.
- Corrected core: run `39133418875` passed 1,832 tests / 362 suites, zero failures/errors/skips. Artifact `12138877302`, 851,783 bytes, SHA-256 `017cc3a013e6fc470592a70b557cab953e957b9f28d1ce8cb7c4bde66dd782eb`.
- Immediate prior compiled APK: verified private TEST279, producer `3b1f29b87d5658bdaf1b7ecc1b019a48955a49d6`, run `39131918637`, APK SHA-256 `4c49b3d43b0bd9b9969353d10df420fd77a9f688f636fee1697930ed68d6642c`.
- TEST280 full run `39134124945` passed 1,832 core tests / 362 suites, debug/history, 38/38 API34 methods and 57 reviewed 320×640 PNGs. APK SHA-256: `634d3ef1826be93ac6af73421257dae09fd2f87940c17a9f81910cde6849c410`.
- Supplemental run `39134124928`: compact, M53, square and 320×480 passed across the initial attempt and one failed-jobs-only retry; short-landscape retained the native-title condition failure. Staging failed closed; nothing was published or distributed.
- Verification receipt: `docs/APD-TEST280-CLERIC-HISTORY-VERIFICATION-2026-10-18.json`.
