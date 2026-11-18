# Continue moving after trap discovery

## Implementation status

Implemented, committed and verified on an API34 x86_64 Android emulator. Source0b4e890de; verifier correction946cc3cea (same application). Full signed-compatible internal build and all three runtime cases passed; downloaded raw evidence checked. Physical-phone testing remains open. Owner APK delivery hold stays active. Two internal attempts were assembled (the first receipt parser failed; the second passed); no owner preview number advanced. A clearly labelled debug application is authorized for the requested Android checks; it must not be published, uploaded as an application artifact, or sent to the owner. Its signing key/application ID stay unchanged. Other approved work remains in progress.

## Actual APD behavior traced

- Hero.handle stores destination commands in curAction (Move STEP/TRAVEL, LvlTransition, pickup/chest/interaction actions). Char.path holds the cached sequence of cells. Held directional input is stored by CellSelector.
- Hero.getCloser performs a legal movement step, pays the original movement delay and then calls passive search(false). Foresight also searches on initial attachment and on standing observation. Intentional searches remain separate.
- Hero.search previously combined visible secret traps and doors into smthFound, showed the notice/sound, then unconditionally called interrupt. Hero.interrupt clears curAction, stores an ordinary unfinished destination in lastAction, resets held input and invalidates trap consent. Thus discovering a nearby trap could stop travel without stepping on it.
- Level.discover changes SECRET_TRAP to TRAP and updates its terrain flags, then reveals the trap. Hidden traps are passable; discovered traps are avoided by normal walking. getCloser rechecks the next cached cell and uses Dungeon.findPath to recalculate a normal legal route. It retains the existing detour-length rejection. Adjacent explicitly selected hazard destinations, fog-trap protection, flight/Amok and trap confirmation retain their existing special rules.
- Actual stepping/occupancy is independent: Level.pressCell interrupts the hero on the triggered cell and calls Trap.trigger. Time-freeze/bubble delayed presses retain their existing path. No trigger, trap effect or hazard rule is patched.

## New option

APD Settings → Gameplay → **Continue when traps are revealed**, Off by default. Its help explains On and Off, normal hazard avoidance and legitimate interruptions. The boolean is stored through the existing flushed preferences system under apd_continue_trap_discovery. It applies immediately to current runs and is not a Custom Dungeon Rule or save-file field.

Search tracks visible non-trap discoveries separately. With On, passive trap-only discovery during movement omits just its own interrupt call. The original destination, queue and held arrows remain. Explicit Search, waiting, doors/other secret discoveries and other interrupt callers keep their behavior. A scoped transient movement-search flag also covers actual walking toward items/chests, and is restored in finally. No cancelled action is restored. Two protected visual hooks retain the original pre-reveal tile/post-reveal particle ordering, enabling headless tests to suppress only renderer effects while exercising actual map discovery. Android checks use the original visuals.

## Local verification

Full local suite: **2,909 tests / 447 suites**, zero failures/errors/skips; `/workspace/apd-trap-discovery-final-full.log`, raw XML `/workspace/apd-trap-discovery-final-local-junit.zip`.22 new cases cover both preference states, actual movement/discovery without stepping, cache/input retention and normal turn cost, trap on route with a safe alternative, trap blocking the only route, real obstacles, actual visible/hidden trap triggering and damage, confirmation, simultaneous door discovery, manual Search/waiting, item/chest/transition travel, enemy/damage/manual cancellation, strict stair safety, fog targets, exploration/step commands, flight, preference-file reload, current-run toggling, Hero save/reload and unchanged detection RNG across40 seeds.

The initial test compile used a nonexistent confirmation cancel convenience method; corrected to the existing choose(false). The first runtime suite then exposed one invalid boundary-writing test fixture; corrected to construct the bulk padded map before building flags. Assertions and production terrain rules were preserved. Logs `/workspace/apd-trap-discovery-targeted.log`, `...-targeted-2.log` and the successful `...-targeted-3.log` are retained. Three tooling tests verify rejection of incomplete, failed/skipped, duplicate and wrong-case instrumentation evidence.

## Android results and boundaries

The dedicated private workflow compiles the complete core/desktop/Android source, builds a single INTERNAL-TRAP-20261107 debug application plus its test harness, verifies package/certificate/checksum, and runs three API34 native cases: actual On tap, an actual force-stop/new-process saved-On check with real Off tap, and real gameplay discovery/rerouting followed by actual Ooze trap activation. Only receipts, raw results, logs and screenshots are retained; no application APK is published or delivered. All stages passed in run42400199124/job128486107636. Downloaded artifact13584978803 (902909 bytes, SHA256efdceaf31f97f3b0e05227126b360ba3b0d743a9ab6ec65604fa95cdd1a8115c) includes exact commit946cc3cea7df43eb2da9036947d114e09bfd22fc, all447 raw core suites (2909 tests, zero failures/errors/skips), three complete native results, signing receipt, logcat and four PNGs. Logcat records separate preference processes2347 and2583 with no FATAL EXCEPTION/ANR. On-control and actual-Ooze screenshots inspected; restart screenshot shows the Gameplay top, so the offscreen checked state is supported by the native assertion, not falsely claimed visible. APK39012742 bytes, SHA25656b2679dc20647c6d0ce2d26b58fbb520e7e888be91d930dc0f7d81c7305cb88; unchanged package/certificate verified. Initial run42399787845 assembled and passed core but its receipt failed on an obsolete Signer #1 prefix; actual tool prints V2 Signer. The corrected parser still requires the exact original certificate. This failure is retained in the verification JSON. These are disposable emulator fixtures, not natural generated-room/seed claims or physical-phone testing.

Intentional limitations: this option does not force movement through a blocked route or change strict stair/wall-search hazard checks. Normal adjacent explicit trap destinations and the separate confirmation preference keep their established behavior. The whole pre-existing mechanics/translation audit and physical-phone acceptance remain open.

## Exact changed files and commits

Production: `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/SPDSettings.java`, `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java`, `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/APDSettingsPane.java`.

Regression/native: `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/qol/TrapDiscoveryMovementTest.java`, `android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/TrapDiscoveryDeviceTest.java`.

Verification: `.github/workflows/apd-trap-discovery-check.yml`, `.github/workflows/apd-qol-source-check.yml` (expected total), `tools/apd_check_single_instrumentation.py`, `tools/test_apd_check_single_instrumentation.py`. Documentation: this report, `docs/APD-TRAP-DISCOVERY-VERIFY-REQUEST.txt`, `docs/APD-TRAP-DISCOVERY-VERIFICATION-2026-11-07.json`, `docs/HANDOFF.md`, `docs/WORK-COMPLETED-LEDGER.md`. Implementation0b4e890de; certificate-parser correction946cc3cea.

Continued prior approved work in7f9eeedd4: frozen/phantom meat floating feedback, real-effect regressions and detailed food-source audit. Latest application source passed2911/447 plus required platform Java checks; those later presentation changes are separate from the earlier native APK. Broader development remains unfinished; no APK delivered.
