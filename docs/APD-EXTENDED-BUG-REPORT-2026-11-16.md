# APD Extended Bug Report: Opt-in action chronology, November 16 2026

## Owner requests
- Keep the previous bug report as Standard.
- Offer a separate Extended bug report.
- Record APD input and actions chronologically between successful Extended reports.
- Record transitions to background, returning to foreground, and wall-clock time away. Do not imply a deliberate quit or infer which other apps were used.
- When enabling Extended logging, warn the player of every category collected, retention and export behavior.
- Default OFF, explicit player toggle/acknowledgement required, and provide a way to turn OFF and remove history.

## Isolated work claim
Branch: `chatgpt/apd-complete-action-report-20261116`.
Baseline: verified C466R7 + C485 combined head `715b7aacdc8759746fbe3f4cc68bae2d34cc9f73`.
No gameplay, balance, scoring, save schema, history codec, signer, package ID or existing APK changes requested.
This branch is independent of scheduled development workers and existing C480 native claims; do not overwrite their refs.

## Design
- `SupportActionJournal` stores a bounded UTF-8 chronology in APD private diagnostic storage, max 64 MiB, and documents coverage limits.
- Explicit flag `apd_extended_bug_logging` is false by default. Logging does not start until the player accepts a scrollable explanation and enables the checkbox.
- Turning the flag off clears retained chronology, even on startup if a prior opt-out was interrupted.
- Records APD pointer positions/presses, redacted key types, control clicks, hero action intent and waiting, setting changes with text values redacted, scene changes, error classes, memory and timing milestones, Android pause/resume and elapsed wall-clock duration.
- No other-app observation, microphone, camera, screen recording, typed input values, passwords, run-history scan, automatic upload, or external network connection.
- Standard diagnostic output preserves its old format and omits the extended journal.
- Extended export snapshots a journal prefix, appends it to the existing standard report, and clears only that prefix after the output stream closes successfully. New events remain.
- A failed or canceled export must not discard evidence.
- Consent warnings and Extended selection are in the shared APD Settings pane, Advanced section.

## Test and release gate
Regression tests: ExtendedBugJournalTest and DiagnosticReportExtendedOptionTest; includes default-off/opt-in/opt-out, interrupted opt-out cleanup, Standard isolation, failure-to-close retention, successful Extended acknowledgement, chronological reporting and pause duration.
CI workflow: `.github/workflows/apd-extended-bug-report-ci.yml`, full core tests plus desktop and Android debug/release/instrumentation Java compile.
Prior run `43927950079` passed against an intermediate implementation before the complete warning and final tests; `43928650165` also passed an intermediate revision. They are NOT proof of the latest source.
Final exact-head verification and native UI acceptance are required before integration and APK release. No APK was built or published by this work claim.
Update this record with verified results, final remote heads and limitations before merging. 
