## 2026-10-10 — UIMSD status and queue-preview windows disappear after scene recreation

- Evidence: canonical `WndUimsdOptions.say/privacySummary` and all three run-detail queue feedback paths construct `WndMessage(String)`; this window has neither a semantic restorer nor a public zero-argument constructor. `PixelScene.saveWindows` therefore selects its reflection fallback, which catches the constructor failure and restores no message. Long queue previews also use an unbounded non-scrolling message window.
- Fix: use the existing scrolling, wrapped `WndUimsdText` window for these five call sites. Its restorer retains the title and exact payload, without replaying enqueue/remove or any other action. No queue/history/save format changes.
- Targeted local evidence: the two changed production classes compile; 35 existing queue persistence/data preservation, window geometry and choice tests pass. New Android assertions exercise the real privacy-summary and generic status call paths, repeated scene recreation, exact text retention, unchanged query identity and a long preview at 100 UI pixels.
- Native/complete build checks pending at commit creation. No physical-phone acceptance claimed. Scope excludes process death and remembering the scroll offset.
- Regression risk: these messages now use the UIMSD scrolling text presentation instead of vanilla small messages. Test the real queue preview and Back on RedMagic after switching apps.

