# APD optional one-tap stair arrival activation

Requested 2026-10-17: when the player chooses **Stairs Up** or **Stairs Down** from the existing shortcut, walk to the discovered stairs using regular turns and then take them without a second tap. This is distinct from globally auto-triggering stair transitions whenever the hero walks onto a staircase.

- New **Auto-enter Stairs on Arrival** checkbox in APD Settings, default OFF. The existing **Automatic Stair Navigation** checkbox still controls shortcut visibility; the new checkbox independently controls floor activation after selecting a direction.
- StairNavigation.Walk carries a transient snapshot of the chosen direction and auto-entry flag. On arrival, Hero validates that this is still the same level, requested staircase type, discovered safe target, with no visible enemies, damage interruption, roots or sealed floor, then calls the existing HeroAction.LvlTransition path. The hero's normal movement and turn cost are untouched.
- When already standing on the chosen stairs, selecting that direction with the new option enabled invokes the same ordinary transition action immediately. With the option off, the old stop-on-arrival behavior stays unchanged.
- Routes still stop on enemies, positive damage, unsafe discovered terrain, traps and player cancellation. Missing/undiscovered stairs are not leaked. The transient route is not serialized; ordinary taps or other movement actions cannot auto-trigger transitions.
- Regression tests cover preference persistence/default, existing shortcut, single activation after regular paid movement, already-on-stair activation, safe stops and manual/damage cancellations. No gameplay balancing/scoring/RNG or save format changes.

Status: source implementation committed for core-only verification. Android/emulator and physical phone are not verified yet. No public release or invite/promotion.

The original two-argument Walk(int,Level) API is preserved as a walk-only overload after first core verification caught its use in existing HeroActionMoveTelemetryTest. TEST273 history is pinned to successful TEST272 run 39066730441. Core rerun 39068161212 pending. No APK compiled for this feature yet.

Core-only run 39068486460 attempt 2 passed all 1786 tests, including existing and new stair tests. Attempt 1 had a separate SeedFloorsTest async notice timing race after all stair tests passed; do not misclassify the attempt 1 suite as success. Full new private TEST273 build requested and pending. Native touchscreen test covers persistence and field owner map. No APK distributed yet.
