> Superseded for scope by the owner’s subsequent clarification and explicit retention reply. See APD-CALCULATIONS-SCOPE-AUDIT-2026-11-07.md. Blanket removal is not the final implementation.

# Ordinary item calculations interface removal — 2026-11-07

## Authorization finding

No explicit authorization for a universal Show calculations interface on ordinary item descriptions was found in the available conversation. No conversation ID or date for such approval can be supplied. The owner authored the earlier broad description request, including “When relevant, the exact formulas and calculated results in an expandable detailed explanation” and “Put advanced mathematical details in a separate expandable section for players who want them.” The visible conversation provides no timestamp for that message. Those words are not an explicit approval of a universal item-window control.

The implementation first appears in commit9749e461ce804328c42e64a9558201d8aaeb04fa, dated2026-11-06T01:33:06Z. Its author identity and commits under the owner's account are implementation provenance, not evidence of owner approval. The implementation expanded the description requirement into a universal interface without an explicit approval record. That scope inference was the assistant's error.

## Exact correction requested by owner

WndInfoItem no longer stores or computes an item calculations supplier. Its existing bounded description layout receives no details supplier, so it adds no calculations button or expanded section. This covers ordinary item inspection and inherited WndUseItem inventory/equipment actions. The item.info() text, description helpers, item actions, existing scrolling, gameplay formulas and save data are preserved. No replacement control, preference, mechanics change, or unrelated interface redesign was added. Shared helper and non-item windows were not redesigned by this correction.

QolItemFeedbackDeviceTest.showItem now asserts both WndInfoItem and WndUseItem contain the exact ordinary item.info() text, including approved improvements; no Show/Hide calculations button; only existing item actions; screen-bounded actions and correctly aligned scrolling; unchanged HP, elapsed action time and serialized item state. This replaces the superseded test expectation that item calculations should expand. Other window regressions remain intact.

## Verification

Full local core suite and required Android/desktop compilation are recorded below when completed. Native assertions are added but are not counted as executed unless an actual emulator/device run completes. No physical-phone, install-over or fresh APK/publication claim is made.

Local full core suite:2933 tests/449 suites, zero failures/errors/skips. Log /workspace/apd-remove-item-calculations-full.log. Exact source platform gate requested; Android runtime/native test not executed at this point.


Exact source-only platform gate42410597354/job128517676080 completed successfully at2026-11-07T20:22:48Z on c8faf5947dc7b27e6f95db65c2946d2a1dcb7e41:2933 tests/449 suites, zero failures/errors/skips, with Android debug/release/instrumentation Java and desktop compilation. Artifact13589865220 is509804bytes, SHA256 53ebf07ebaab70581bc6085355f127a2b0828975428bf9e9dcb3436f3510c486; embedded source receipt and all449 raw XML suites downloaded and checked. No APK assembled; native runtime and physical phone remain untested.
