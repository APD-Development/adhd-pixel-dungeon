# APD fallback history ZIP picker, November 12, 2026 (Eastern)

## Scope and coordination

This is work performed in the originating regular ChatGPT conversation against the owner's connected GitHub repository, without consulting upstream or other external APD resources. The established authoritative development branch was confirmed at `1eff9f6e3698e6a8804e82683e0821ea78d91c65` before the isolated branch was created, not the repo's default `main`. AGENTS.md, the completed-work ledger, known-issues guide, archive tests, recent history-index continuation, existing PRs, CI runs, and APK release metadata were inspected. PR #53's direct-run-index guard and PR #54's receipt were already merged; their investigation was not restarted. Independent Android playback, native acceptance and release work remain untouched.

Isolated work branch: `chatgpt/apd-folder-import-mtime-20261112`. [Draft PR #55](https://github.com/APD-Development/ShieldingShouldHaveWorked/pull/55) targets `codex/apd-phone-fixes-2026-10-09`. No shared-branch write, force push, APK build, publication, signer change, gameplay rule or scoring adjustment.

## Evidenced source discrepancy and correction

`ApdFiles.FolderProvider.chooseImport` explicitly promises to open the newest `.zip` from the desktop/fallback `imports/` folder, but was previously sorting filenames alphabetically and selecting the last name regardless of file modification time. An older `z-older.zip` was therefore chosen over a genuinely newer `a-newer.zip`. This is a bounded source-demonstrated implementation mismatch, not a claim of corrupted owner data.

- Production commit `7f705a2a1397eb9b2625d25c57571aaed09ea22f` selects the eligible ZIP with the newest modification timestamp. Existing alphabetic selection is retained for timestamp ties. File filtering, folder fallback, stream ownership and Android's own system document provider are unchanged.
- Test commit `a5527fdd52cec5abe3bb4f5ea01bf8b4c3b84fde` adds two real-filesystem JUnit cases: newest timestamp defeats inverse alphabetical order and non-ZIP files, and equal timestamps retain the former deterministic name preference.
- CI workflow commit `b3d07b1562a96ff4224e6a5bf9c9152d61872f2f` adds a branch-scoped full-core, desktop and Android debug/release/instrumentation Java source gate. It requires both exact regression test names, at least 3,330 tests and zero failures/errors/skips.

## Verification state

[Exact-source GitHub Actions run 43341753462](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43341753462) was observed **in progress**, against `b3d07b1562a96ff4224e6a5bf9c9152d61872f2f`. Do not describe this as a successful test run until the terminal workflow, job and JUnit gate are inspected. No local Gradle/Android SDK, emulator, original-signer APK, physical RedMagic test, or new release was executed by this work.

APD464 was independently published before this work; it is not a build containing this fix. Existing source, save, scoring, history, APKs and held work remain intact.

## Next safe work

Read exact terminal CI and test artifacts, correct any evidenced failures without weakening gates, read current primary and PR head and conflict state, and only then integrate with a non-force expected-head check. Preserve a terminal result and accurate start/end evidence here. Do not claim work continues between executions.

## Exact CI receipt, 8:05 PM EST

[GitHub Actions run 43341753462](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43341753462), attempt 1, completed **success** on exact source/test/workflow commit `b3d07b1562a96ff4224e6a5bf9c9152d61872f2f`. Decoded job `131611476473` reports `BUILD SUCCESSFUL in 2m 41s`, then the required JUnit gate confirms **3,330 core tests across 480 suites, zero failures, errors or skipped tests**, including both new real-filesystem regressions. Desktop Java classes and Android debug, release and instrumentation Java compilation succeeded. JUnit artifact `13999063709` (`apd-folder-import-mtime-junit`, 537,533 bytes) was uploaded, GitHub SHA-256 `14fdeece464f6c615abfc92bf826d1d76e6b16cc736064ef61faf04fa9551a37`.

The log contains expected exception traces printed by negative-case tests; they do not represent failed test results. CI did not build any APK, run emulator/device native cases, verify an original signer or publish a release. Later documentation-only commits do not change the exact tested application source, regression code or workflow. Preserve the distinction when integrating.

## Final guarded source integration, 8:06:57 PM EST

Before integration, independent live reads confirmed primary branch `codex/apd-phone-fixes-2026-10-09` remained exactly at starting commit `1eff9f6e3698e6a8804e82683e0821ea78d91c65`, candidate PR #55 head was `e7848c44bcff59041fe19b8a2cfdadfa62b7915a`, and GitHub reported `mergeable=true`. PR #55 was marked ready after successful CI. The connector merged via non-force `merge` with **expected head SHA `e7848c44bcff59041fe19b8a2cfdadfa62b7915a`**. The API returned `merged=true`, commit `db6e35841b9b474214000eddfee5c1f71e59155d`, committed **2026-11-12 8:06:57 PM EST** (2026-11-13 01:06:57 UTC). Independent readback showed the live authoritative primary head equals `db6e35841b9b474214000eddfee5c1f71e59155d`, PR #55 is closed and merged, and its recorded merge SHA matches exactly.

The starting source head was `1eff9f6e3698e6a8804e82683e0821ea78d91c65`; the production-tested head was `b3d07b1562a96ff4224e6a5bf9c9152d61872f2f`; the source integration head was `db6e35841b9b474214000eddfee5c1f71e59155d`. Later commits before merging touched only the continuation and AGENTS index, so the exact successful source code/test/workflow remains unchanged. No shared branch history was overwritten. No APK was built or published during this execution: APD464's original Developer APK and source asset remain previously published, without the new desktop fallback correction. Private signing inputs, native screenshots, emulator/device acceptance, and complete release gating were not exercised for this fix. Other active APK/animation claims and held candidates remain untouched.

This final integration receipt is added on an isolated, source-neutral documentation branch. Subsequent work must verify its actual merge separately; it does not authorize a gameplay or release action. Development does not continue after this execution except through separately running authorized automation executions.
