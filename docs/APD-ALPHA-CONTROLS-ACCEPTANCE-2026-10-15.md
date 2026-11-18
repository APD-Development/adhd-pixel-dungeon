# APD all-control acceptance — October 15, 2026

## Native acceptance on the verified application

[Run 38802091366](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38802091366) reused the exact verified TEST217 application:

- application source: `d2c28cb076d9918632d313e5a4f9655d36c9433b`
- application run: `38795254303`
- APK SHA-256: `4db6d38ad6ff0a6a5e375c43fadfa990b52172b2b14aac4bb2dafce9d9e6a6e9`
- acceptance test source: `b2d4807502774c21e30b5bb3ae77eee765de1dfd`
- evidence artifact: `12005075844`
- artifact SHA-256: `a89053e414966efdd622f0b2f8edffca7b8eec72b2237b063c47656f6d9bd228`

The API34 instrumentation reported `OK (1 test)` with zero failures, errors or skips. It used native touchscreen events to set every one of the 18 controls to its maximum, saved, reopened and checked every value, then repeated the process at each minimum. Draft edits did not alter preferences before Save; neither phase changed the locked active-run object or the earned score.

This is emulator evidence. It does not claim a physical-device result, signing comparison, owner-save installation or publication.

## Recorded-field and export acceptance

Two retained core tests use genuine run-log recording and cover:

- all 18 recorded fields at minimum, neutral and maximum;
- historical records with absent settings remaining unknown;
- equality, inequality, less/greater comparisons, inclusive bounds, between, known and unknown operators;
- combined Enemy HP and Upgrade Scroll filters;
- run details and side-by-side comparisons;
- Markdown, plain text, HTML and CSV exports;
- original earned score retention and unavailable unapproved multiplier/adjusted score for non-neutral runs.

Review found that the plain-text table renderer omitted the first column's label. The bounded correction labels every non-empty column, including the first, without changing data, order, history or any other export format.

## Verification boundary

The separate reuse run remains useful direct native evidence, and TEST218 subsequently verified the complete corrected source. Exact source `2210d59e1924fdf5ff56ecfb41c25ee0b2c707d0` / [run 38804215957](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38804215957) passed 1,656 core tests in 327 suites, debug assembly, complete immediate-previous TEST217 history, 29 API34 methods and 38 valid 320×640 screenshots. The full contact sheet was reviewed readable.

Artifacts: core `12005990913` / SHA-256 `510449c48e56a8adbedf45d9d24c7a9b0a2df947e4216024accdf18d3615b509`; APK/history `12006690105` / `ac73a551ffebcc2d4abaecfa35f18fed0e63d9b16841f61f161ef786af2f28be`; Android `12006182839` / `978a840e8949558462e2ed1713fe5a2bbb07b3666433adeea258d93adc2ef48e`. APK payload SHA-256 is `184f35fc0371d07d860365f903825d37a0d759d0192e36bb2627cf8d98d04898`.

All slider ranges remain provisional. No rule value, gameplay behavior, score, save, history, turn, RNG, signing identity or application ID changed. Delivery was skipped and no physical-phone result is claimed. Later healing-conversion and authored-language source is outside TEST218 evidence.
