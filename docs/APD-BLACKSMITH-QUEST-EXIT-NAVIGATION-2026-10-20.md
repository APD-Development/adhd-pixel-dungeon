# Blacksmith and special-branch stair navigation — 2026-10-20

## Result

Core-verified application source: `f6c0552e31fc23853521d36b90723eeac8e3cef4` (parent `4a61260c156a0a7ad19e9d3db4f7542732824204`).

The stair-navigation shortcut now derives its offered action from the level transition contract. A level containing a `BRANCH_ENTRANCE` offers one action, **Return to Main Dungeon**, and routes to that discovered quest exit through the existing safe stair walk. Main-dungeon `BRANCH_EXIT` cells do not replace the ordinary **Stairs Up / Stairs Down** menu, so normal behavior returns immediately after the hero leaves the branch.

## Confirmed cause and bounded fix

`StairNavigation` previously matched only `REGULAR_ENTRANCE`, `SURFACE`, and `REGULAR_EXIT`. `StairNavigationTag` independently hard-coded the two ordinary directions. Blacksmith's mine instead exposes a `BRANCH_ENTRANCE` back to branch 0, so the shortcut had no truthful branch-return model.

The fix adds an explicit `RETURN_TO_MAIN_DUNGEON` route, selects it only when the current level actually contains a branch entrance, and carries that exact route through destination discovery, walking, arrival revalidation, and optional auto-entry. Existing enemy, damage, rooting, floor-lock, unsafe-tile, route-blocking, discovery and transition-identity guards remain active.

## Similar-area audit

Exact current source was checked at the final branch checkpoint:

- `MiningLevel` / `MineEntrance`: branch entrance returns the Blacksmith quest to the main dungeon.
- `DeadEndLevel`: branch entrance returns the Imp special area to the main dungeon.
- `VaultLevel` / `VaultEntranceRoom`: branch entrance uses the same return contract.
- Main-floor `BlacksmithRoom` and `AmbitiousImpRoom` create `BRANCH_EXIT` transitions into their branches; those are deliberately ignored by return-menu selection, preserving ordinary Up/Down navigation on the main floor.

No special-area transition or quest completion logic was rewritten.

## Tests

New suite: `StairNavigationBranchTest`, six methods:

1. branch areas offer only the return route;
2. the return route uses the existing safe path walk;
3. auto-entry activates the branch entrance exactly once;
4. main-floor branch exits restore ordinary Up/Down routes;
5. an undiscovered quest exit does not expose its cell;
6. a changed transition identity cannot auto-enter.

Initial run `39505527082` executed all 2,019 methods; production compiled and 2,018 passed. The sole failure was the new auto-entry fixture's missing no-op `HeroSprite`, causing a headless null dereference before the assertion. Artifact `12304100040` was retained (959,516 bytes; `sha256:39eccc2a79df880333b59bd0dfb5c0aa84e16277caad9b5d11205238c73390e8`). The fixture was corrected in `b4c92ca76710a51076dfcfafb72722fb5e175965` without changing production behavior or assertions.

Final verification run `39506031984` succeeded at exact checkpoint `b1a534ac9c1ab64dedf47b1ce8dfbefc97e03141`. Artifact `12303236898` is 953,932 bytes with `sha256:06dcf42aacc52a2bd51fd896e4244a8cccaf2100901f9d717ea01d26025ba834`. Extracted XML reports:

- 2,019 tests
- 393 suites
- 0 failures
- 0 errors
- 0 skipped

## Boundaries

No APK was requested or built. `docs/APD-CODEX-BUILD-REQUEST.txt` was untouched. No emulator, API34, display, or physical-device evidence is claimed. TEST303 remains the newest produced paired APK milestone; the latest actually published owner APK remains Alpha Preview TEST262 (published 2026-10-16). The Wraith report remains a separate investigation and received no speculative mechanics change here; this verified fix is ready to be included with that work in the next owner-requested test build.
