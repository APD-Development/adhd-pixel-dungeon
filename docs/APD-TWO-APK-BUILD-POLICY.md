# APD two APKs every time

Owner requirement: for **every** Codex private test build, create both the developer/test APK and its **matching regular-player APK**, from the **same exact source commit**.

The existing `codex-sprint-check.yml` builds the developer APK when `docs/APD-CODEX-BUILD-REQUEST.txt` changes. The paired `codex-player-with-every-test.yml` now responds to that **same exact request**, waits until the developer artifact for the same source succeeds, extracts and matches its complete embedded offline history, runs core tests and compiles a regular-player debug APK **without `-PapdTestBuild`**.

Both versions must retain the same application ID, signing certificate and history; the player version must exclude the developer-only `assets/apd/features.txt` marker. Keep separately numbered artifacts. No automatic GitHub Release, public staging or tester distribution.

This workflow is deliberately NOT triggered by its own creation so it does not generate another unnecessary developer build. The TEST285 paired player APK was produced separately from the exact TEST285 source in run [39244291635](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39244291635) following repeated owner requests; the developer APK was produced in [39187804530](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/39187804530).

If one paired build fails, report that specific failure and keep the other private artifact; never claim both completed when only one did.
