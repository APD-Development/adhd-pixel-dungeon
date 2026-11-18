# APD TEST75 candidate — October12,2026

Owner approved0.5x/1x/2x playback-only game speed. Added compact APD Settings button and three native choices. Preference persists without changing save format, application ID or keys. Invalid stored values return1x. Default1x is unchanged SPD timing.

Only dungeon visual groups scale their elapsed frame duration. The original frame duration is restored in a finally block before HUD siblings update. Nested visual groups scale once. No global Game.timeScale, Game.timeTotal, actor time, movement costs, enemy AI, damage, cooldowns, pathfinding, RNG, stats calculations or recording ruleset changes. Menus, long presses and UI input stay at normal speed. Sound/music tempo is not modified. Effects that deliberately use the absolute real-time clock retain their existing pacing.

All changed production Java compiles locally against the existing dependencies. Eight permanent tests passed0.068seconds: actual settings-file reopening, invalid stored/set values, exactthree multipliers, nestedgroups, HUD sibling clock, exception restoration, nativeTweenercompletion at30/60/90/120/144/165FPS, unchangedActor.now/globalclock. Initial local compilation lacked libGDX/Cleric dependency classpaths; using the already available dependencies resolved it. This was a local harness setup error, not an app bug.

Android fixtures strengthened without reducing existing assertions: real pointer events choose each option and reopen settings; real dungeon sprite-group Tweener completion timestamps compare slow/normal/fast and assert no consumed turns. Full current core/debug/API34 checks requested, not yet completed; no signing check/publication claim. TEST73 remains delivered playable rollback. TEST74 verification in progress for the allocation cleanup; preserve its produced compiled history independently even if later checks fail.

Comprehensive UIMSD audit/redesign and physical-phone verification remain unfinished. Owner4PMEDT target is a target, not proof of completion.
