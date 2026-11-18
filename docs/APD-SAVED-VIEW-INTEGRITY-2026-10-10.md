## 2026-10-10 22:31 UTC — Saved-view corruption protection

New deterministic regressions confirmed a saved-view data-loss bug: load skipped damaged/duplicate rows, then a normal save, rename or delete replaced the original file with only the readable subset. Invalid UTF-8 hidden inside valid Base64 was also silently converted into replacement characters.

Fix: reject replacement after any incomplete load before creating a temporary file; decode Base64 payloads with strict UTF-8. Valid views remain readable and UIMSD explains why edits are paused rather than reporting a corrupt file as an empty history. Unsupported but structurally valid future queries are retained byte-for-byte and do not block saving other views.

Evidence: regression failed on baseline for malformed replacement; separate invalid-UTF-8 case failed before strict decoding. Final local targeted suite: 39 tests passed (saved views/model/combined scope/settings). Initial broad invocation named nonexistent SavedQuestionsTest and executed no substantive checks; corrected invocation is the evidence, not that failed attempt. No native/complete build evidence yet for this new fix. TEST27 verification of the earlier dialog fix is independent and pending.

