# TEST272: 1000x hero idle head-shake fix (private developer speed experiment)

Owner report (2026-10-17): at developer-only 1000x, the hero's head shakes violently. HeroSprite's idle animation alternates frames 0 and 1 at 1 fps; PlaybackGroup multiplies its global visual delta by 1000 for the mob group, causing idle-frame aliasing at real render rates.

Targeted change: Only when developer visual playback exceeds 2x, HeroSprite.updateAnimation runs its idle-frame advancement using the original real-time delta supplied by PlaybackGroup.heroIdleElapsed(); it restores Game.elapsed in a finally block before the rest of the scene updates. Underlying attack, run, movement, visual effects, game actor clock, input/HUD clock, scoring, saves, RNG, and public 0.5x/1x/2x settings are unchanged.

Regression: PlaybackSpeedTest.extremeVisualPlaybackPreservesNormalHeroIdleTiming exercises all ten settings inside a real PlaybackGroup, unchanged original speed at 0.5/1/2x, clamped idle delta at >2x, and untouched global elapsed. Also retains existing actor/global isolation tests. This does NOT establish physical-phone visual acceptance; owner must confirm head stability on device.

Verification: core-only requested, pending. No APK, download, release, or private stage authorized by this core-only request. Prior verified TEST271 remains usable by switching down from 1000x.

Core-only run 39066269406 passed but its initial test fixture accidentally parsed literal backslash-n, silently exposing only the first three modes; that pass does NOT count as coverage for experimental 1000x. Corrected fixture core-only run 39066377644 passed. The latest test source adds an explicit required count of ten modes (run 39066528447 pending), so this coverage mistake cannot recur. Full private TEST272 build requested using TEST271 as immediate previous verified APK; no publication or physical-phone acceptance yet.
