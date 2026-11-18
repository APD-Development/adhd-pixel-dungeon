# APD Markdown table carriage-return boundary, November 13, 2026 (Eastern)

## Isolated work claim, approximately 04:20 EST

Continuing the originating regular ChatGPT APD conversation. Authenticated GitHub readback verified the authoritative development branch `codex/apd-phone-fixes-2026-10-09` at `b99f459e583f435fd34ecd223d2041e36af6158b` after the completed CSV metadata fix (PR #104/#105). Independent work branch: `chatgpt/apd-markdown-cr-table-boundary-20261113-0419`, created from exactly that commit. Reviewed root `AGENTS.md`, work ledger, reports documentation, recent continuation receipts, open PRs and releases. Existing draft PRs claim unrelated playback, native UI, release, GPL, search and challenge packages; no live open PR claims this narrow Markdown carriage-return table-cell boundary.

**Source-evidenced bug:** `ReportRenderer.md(String)` doubles backslashes, escapes pipes and flattens LF but does not flatten CR. An exported Markdown table header or cell containing standalone CR or CRLF can leave a physical carriage return inside the GFM table row, disrupting its layout. This is a source-code finding, not an assertion about an observed owner report or a tested external Markdown parser. The earlier October 16 Markdown audit addressed backslash loss, not CR; preserve those verified escapes.

**Bounded correction:** Flatten CRLF together and standalone CR to one space while preserving the existing LF and backslash/pipe escaping, all source report cells, report provenance, other formats, gameplay, scoring, history/saves, signing, IDs, editions and existing releases. Add focused JUnit regressions and exact-source core/desktop/Android Java compilation. Do not overlap active work packages or build unqualified APKs.

**At claim:** no production fix, JUnit verification, original-signed APK, native acceptance or release from this claim. APD464 and existing work claims remain unchanged. Integrate only after final CI and live expected-head non-force verification; preserve any concurrent authoritative changes.
