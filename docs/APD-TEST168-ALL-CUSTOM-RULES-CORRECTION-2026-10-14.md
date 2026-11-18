# TEST168 all Custom Dungeon Rules acceptance correction — 2026-10-14

## Failed evidence retained
- TEST167 source `57b29f9ec716aabb20237a963fe8d32e4bbfb930`, run `38626757231`, ran 1,585 core tests with 2 failures. No debug APK, API34 run or screenshots were produced.
- Failure 1 was fixture-only: a real enemy attack against the headless `FixedHero` entered the visual `Hero.interrupt()` path because `damageInterrupt` retained its gameplay default. The fixture now disables that visual interruption; real attack, damage replacement, Player Defense, source and healing checks remain unchanged.
- Failure 2 was a manifest-only invalid native sprite enum (`V061_CHANGE`). It is replaced with existing `V064_CHALLENGES`; no historical coverage guard is weakened.

## TEST168 request
- Rerun the corrected exact source with all 1,585 core tests, debug assembly, complete reviewed history against immediate previous compiled TEST166, 26 API34 emulator tests and 34 screenshots.
- All 18 controls remain a candidate until those checks pass. Active-run controls stay locked; original earned scores and scoring identity remain unchanged.
- No delivery, public release or physical-phone claim.


## Actual TEST168 result
- Corrected source `3a70f85f706dcf56553af296c1a44f98635a5af7`, run `38627408866`, ran all 1,585 core tests with 1 failure. The native history sprite correction passed.
- The interaction progressed through the initial hit but `Hero.damage()` independently invokes `interrupt()` after serious damage, reaching the same absent headless selector. TEST169 overrides only `FixedHero.interrupt()` as a no-op; real attack, damage, mitigation, source and recovery paths remain intact.
- Debug APK, API34 and screenshots were skipped; no TEST168 APK exists. TEST166 remains latest verified.
