# TEST198 — shop-stock visibility boundary

Date: 2026-10-15
Branch: `codex/apd-phone-fixes-2026-10-09`
Parent: `c03905674d306d3d95f8c90d04a3a5e522d5349d`
Compiled application source remains: `364fd4724b79308fb6a13478a211bcd7599b2f7b`

## Reproduction

The queued shop-stock candidate already has a canonical recorder path. At each ordinary item poll, `ItemTracker.scanFloor` considers only a heap that the player has seen, is not a faint hidden outline, is currently inside `heroFOV`, and exposes its top item. A `FOR_SALE` heap is classified as `ItemEvents.SEEN_SHOP`; chests and unseen or out-of-view heaps remain excluded.

The concrete omission was retained coverage. Existing tests verified ordinary floor items and hidden heaps, but did not bind shop classification to the public/internal information boundary.

## Change

Added `ItemLifecycleTest.visibleShopStockIsClassifiedWithoutLeakingHiddenTruth` using a real unidentified +4 cursed `RingOfWealth` in a visible `FOR_SALE` heap.

The regression requires:

- exactly one public `ITEM_SEEN` with `SEEN_SHOP`, floor and cell;
- only the displayed unidentified ring name/category, with unknown level and curse;
- no “Wealth” class-name leak in public state;
- the true class, +4 level and curse only in the separate INTERNAL `ITEM_REGISTERED` event.

No production source, telemetry format, gameplay, RNG, save, scoring, rules, application ID or signing behavior changed.

## Verification request

Run the complete 1,611-test core suite through the existing core-only workflow. Do not build an APK; reuse the already verified TEST197 application artifact. Hosted result is pending.


## Hosted result

Core-only run [38700157769](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38700157769) completed successfully from 2026-10-15T07:38:53Z through 2026-10-15T07:41:19Z at exact test/docs source `bed2aa8513e1ab7ac5b307f8a04d9f0fda9a2d33`.

- Checkout and Android SDK setup passed.
- `./gradlew :core:test` completed with `BUILD SUCCESSFUL`.
- The new `visibleShopStockIsClassifiedWithoutLeakingHiddenTruth` regression compiled as part of the suite and the complete job passed.
- Artifact `11955827287`, 745,760 bytes, GitHub digest `sha256:2a0d77bf965610aefb181a6d12355697f39b8a86a449e6cbcc6e7818f5e3d2a1`.
- The requested suite count is 1,611 (verified TEST197's 1,610 plus this one retained test). Independent XML recount could not be completed in this runtime because every host-file proxy download attempt returned HTTP 502; do not present the configured count as a separately parsed artifact count.
- No APK job existed and no APK was rebuilt.

The regression gate passed. Production application source and verified APK evidence remain TEST197/Codex TEST 194. No physical-phone or delivery claim.
