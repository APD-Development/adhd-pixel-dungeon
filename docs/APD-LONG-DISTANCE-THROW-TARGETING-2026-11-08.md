# Direct throwable targeting — November 8, 2026

Owner request: directly targeting a visible enemy must find a trajectory already available by aiming beyond it. Preserve range, damage, accuracy, collisions and other targeting systems.

## Reproduced cause

Ballistica rasterizes a straight ray using the slope to the selected aim tile and an initial integer error of half its major-axis distance. Aiming beyond the enemy changes that slope and rounding, so one legal ray can enter the enemy's tile while direct aiming collides earlier. MissileWeapon previously tried only the direct ray and a clear reverse-rounding ray. That handles an earlier tie case but not all existing beyond-target rays. QuickSlotButton additionally tries nearby aim cells within two tiles, but manually targeting the enemy still uses MissileWeapon.throwPos.

Concrete fixture: source (3,3), enemy (25,12), wall (24,12). Direct and reverse rounding are blocked. The unchanged Ballistica.PROJECTILE aimed at (26,12) hits the enemy before any obstacle. The new test failed against the original code (`before.log`), returning the preceding collision tile instead of the enemy.

## Exact source changes

- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/weapon/missiles/MissileWeapon.java`: after its existing direct/reverse handling, a blocked throw at a currently visible enemy can use an already legal alternative aim. Projecting and Shared Enchantment branches remain intact. Spirit Bow arrows are excluded as before. Empty tiles, unseen enemies, ordinary items and wand targeting retain their existing paths.
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/qol/ThrowableTargeting.java`: searches existing in-map aim cells without a distance cap. A cheap filter uses the current raster's integer stepping to reject aims that cannot visit the target; qualifying candidates are validated using the **unchanged** Ballistica.PROJECTILE. A candidate succeeds only if its first collision is the enemy. No terrain, visibility, actor time, item state, save data or RNG is changed.
- `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/MissileLineOfFireTest.java`: five additional tests, covering the reproduction in eight octants and quick-slot targeting; 64 deterministic obstacle layouts comparing the filter against every in-map aim using actual Ballistica; distant intervening creatures/solid barriers; unchanged Item/Wand/SpiritArrow/empty-aim/unseen-target behavior; and no turn/HP/RNG changes. The six existing tests, including intentional Projecting behavior, remain.

`Ballistica`, `Item.cast`, damage/accuracy calculations, wand/bow implementations and projectile collision flags were not edited. The correction returns the existing collision target through the existing throw API; it does not change the attack's chance to hit after reaching that target.

## Verification and limits

Full core suite: 3,021 tests across 456 suites; zero failures, errors or skipped tests. All eleven MissileLineOfFireTest tests passed. `git diff --check` passed. Raw XML, before/after/targeted/full logs and count receipt: `/workspace/apd-long-throw-evidence-20261108`.

This is a source/test fix. No new APK has been built or published, and no Android device test or rendering verification was run. This environment has no Android SDK/adb/emulator. The fixture reproduces the reported behavior independently; the owner's original phone save and marked screenshots were not available with this latest request. The preceding description-audit source is preserved; this specific targeting fix changes no wording or unrelated mechanics.
