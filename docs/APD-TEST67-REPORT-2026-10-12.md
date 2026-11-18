# APD private TEST67 development report — October 12, 2026

## Candidate and verification
Application source: fa76dede19aa1a207531413b9de838b1d40fefa0. Full verification run: 38248003026.
Actual downloaded core XML: 1,150 tests across 198 classes, zero failures/errors/skips. Actual downloaded Android XML:19 API34 cases, zero failures/errors/skips; full required run succeeded. Actual TEST67 PNG inspected. The delivery validator appends actual Android results, APK checksum, certificate, package and run timestamps.
Last delivered APK is TEST63, source1f0c843f510957d77702a26c458fd7336d1f56b9. TEST64–66 were intermediate artifacts, not delivered releases.

## Changes since delivered TEST63
- TEST64: replacement UIMSD Home/table contents destroy their owned image resources. Native Group.clear detaches children without destroying drawn image buffers; an API34 test reproduced the leak against the actual TEST63 APK before the scoped fix.
- TEST65: reserved recorder close-control slots prevent a saturated flush queue from stopping shutdown/sealing. A deterministic stalled-writer regression failed on the baseline; 14 targeted writer tests passed after the fix.
- TEST65–66: Changes uses original SPD ChangeInfo/ChangeButton patterns, native title/category colors/grey navigation, horizontal category icons and paired columns. Six distinct builds appear per bounded page. Every entry popup identifies its exact APK. Existing descriptions remain available. Real screenshots of APD and the original SPD ChangesScene were exported and inspected; the latter uses APD's current asset palette, not a separate vanilla APK.
- TEST65: reconstructed recent histories for actual APKs TEST51/54/55/58/60/61/62/63/64 using artifacts, successful assembly/upload steps and adjacent APK-producing source diffs. Failed attempts without an APK are not assigned changes. Future builds retain previous compiled histories.
- TEST67: pickup refusal explicitly says to recover the lost backpack when actual LostInventory blocks a nonretained item. Previously capacity and the status restriction shared the misleading cannot-carry message.
- TEST67: clear-after-destroy of UIMSD-owned contents remains a no-op, matching native Group behavior. Previous helper accessed a null members list after destruction; a permanent deterministic regression reproduced NullPointerException.

## Preserved behavior
No inventory capacity, collection rules, LostInventory application/recovery/expiry, save formats, statistics calculations, gameplay timing, combat, RNG or identification rules changed. No player save was edited or item deleted. Classic inherited SPD bag quickslots remain; owner withdrew Custom-mode bag work. Main and Claude branches were not overwritten. Signing identity and application ID are checked directly before private publication. No unfinished online features were activated.

## Evidence and failures
TEST64: 1,139 host +18 API34 cases passed.
TEST65 and TEST66: 1,145 host +19 API34 cases passed; no failures/errors/skips.
TEST67: actual1,150 host and19 API34 cases passed. Five permanent new host cases cover destroyed-content lifecycle and context-sensitive pickup wording. Local combined ownership/inventory set22passed0.403s. Additional reviewed saved-view/export/transfer set17passed0.484s, and per-unit/column/report/scope set14passed0.647s. These local checks supplement, not replace, exact-source CI.
Screenshot export initially produced error text rather than PNG despite passing UI assertions. A second shell-based fixture failed; changed to MediaStore and strict PNG/dimension validation. Exact-TEST65 screenshot-only reuse succeeded without rebuilding application code. TEST66 exported actual valid screenshots; both were inspected. Those failures are not counted as successful visual checks.
An encoded-slash branch read was rejected; the branches collection confirmed the approved real head. No synthetic local Git history was pushed.

## Inventory report limits
The owner's TEST50 screenshot shows empty slots and the LostInventory indicator. All reported item types are rejected under that intended restriction even with free backpack/pouch slots. The owner later recalled possible Ankh use. Existing12collection/save/recovery regressions pass; no stuck-expiry/capacity defect was established. The clearer message does not prove why the owner's run entered the state or that an unexpected state transition is fixed. Real-owner-save/physical-phone recovery remains unverified.

## Potential regressions and remaining work
Owned child destruction must not dispose shared resources; targeted ownership and drawn-image Android regression cover the scoped path. Late cleanup must remain idempotent. Pickup explanation must not falsely blame retained items or expose unidentified properties; it selects only the existing restriction.
Older build history remains incomplete where evidence is missing; no speculative changes are assigned. Not every historical APK binary was independently decompiled. The whole UIMSD audit is incomplete. Full normal dungeon play, physical Pixel/Android17/PixelBuds, real-save upgrades, every challenge/filter/item combination and long-term low-end-device stress are not established by emulator acceptance.
Source files changed across this delivered batch: UimsdContent.java, UimsdHomePane.java, UimsdTable.java, RunLogWriter.java, APDTextScene.java, ApdHistory.java, Hero.java, actors.properties, offline history assets, history-maintenance tool/workflow and retained host/Android regression fixtures. Relevant application commits:41852947416a06a9067c6e6c651ed3729969347d,4ffe4fb38533e41ad17c4c1dd3ebc528f55ace0a,21be7de2fcec65441fe954babe2dc34142a26ce7,fa76dede19aa1a207531413b9de838b1d40fefa0.

## Timing and installation
CI start/end timestamps come from GitHub and are appended by the validator; CI time is not claimed as actively coding. Owner stopped development; only the requested read-only test audit ran until explicit resume. No continuous unattended coding has been demonstrated.
Delivery reuses the successful exact-source APK without another application build. Install over the existing APD app; do not uninstall or clear data. Signature/package/version compatibility is checked against the actual preserved TEST50 APK. Automated compatibility is not physical-phone save acceptance. Preserve previous private assets.


## Delivery preparation failure
First deliveryrun38249478293 passed source/tests/checksum/signing/package checks and stagedAPK/PDF, but publication refused because copied release-tag/APK filenames still pointed atTEST63. Existingassetswerepreserved. Publicationidentifiers now derive from APD_TEST_NUMBER; the corrected delivery reuses the same verifiedTEST67APK withoutrebuild. This workflow failure was not an application test failure.
