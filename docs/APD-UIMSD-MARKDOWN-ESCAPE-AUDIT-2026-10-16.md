# UIMSD Markdown cell escaping — October 16, 2026

## Verified root cause
The current table-cell encoder escapes pipes but leaves existing backslashes unchanged. An independent Pandoc GFM render of actual ReportRenderer output confirmed that an input heading `Kind\\|Name` displays as `Kind|Name`; `one\\|two` loses its backslash and `folder\\\\file\\*x` loses literal backslash characters. This is exported display text only; report objects and raw player history are unchanged.

Correction to the earlier audit premise: the tested GFM parser did NOT produce an extra table column. Its table pass consumes a pipe escape before inline Markdown processing. The confirmed defect is literal-character loss, not a verified structural column split. Do not repeat the earlier column claim as evidence.

## Small correction
Double existing backslashes before adding pipe escapes. Preserve the existing newline-to-space behavior. No parser, export format, scoring value, ruleset, save, cache or raw history format changes. Ordinary text and other export formats retain their existing output.

## Actual regression evidence
Two permanent JUnit methods render real Report tables, checking literal backslashes in headers and values, pipes, line breaks, empty/null cells and preservation of source values and text/HTML/CSV output. On uncorrected ReportRenderer:2 methods,1 pass/1 failure/0 errors. After correction:16 focused report/export methods pass, including previous CSV, stream-completion, run-identity and repeated-label comparison regressions. These are local host tests, not Android phone evidence.

A real Java-generated Markdown document was parsed with installed Pandoc's GFM reader, then its HTML table cells were inspected. Before:expected literal values did not match; after:all six header/data values matched, with original table shape preserved. No fabricated parser or production dependency was added. The local artifact is /workspace/scoring-preview/report-markdown-parser-evidence.json. Earlier local multi-suite invocations encountered a missing previous test class and then an obsolete local ReportBuilder; syncing the already-committed TEST255 class resolved that setup mismatch. Those failed local invocations are not called successful checks or new production defects.

## Required verification and risks
Complete core/debug/history and native Android verification are requested against the next exact source. Expected1754 core methods,37 Android methods and55 captured screens. Until those actual results and visual review pass, this candidate is unverified; delivered TEST258 remains the playable fallback. Any failed checks must remain recorded.

Potential regression: Markdown readers may differ in escape handling; verified behavior is Pandoc GFM, with permanent exact encoding assertions. CSV, HTML and plain-text paths are unchanged. No physical phone, document-provider export or real-player external Markdown app has been tested in this session. No redundant application APK is requested for documentation-only evidence.
