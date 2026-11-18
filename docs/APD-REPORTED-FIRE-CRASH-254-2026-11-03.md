<!-- ADHD Pixel Dungeon / Copyright (C) 2026 Chris Charlton. GPLv3 or later; no warranty. See LICENSE.txt. -->
# Unconfirmed older-build crash while burning — November 3, 2026

Status: OPEN / NOT REPRODUCED. No fire-specific fix is claimed or packaged.

## Report

Owner supplied screenshot18319.jpg of a tester reporting one crash after catching fire, then having to redo the floor. Tester tentatively identified their APK as the first one supplied, probably254. They did not capture a bug report and described this as their first such crash in roughly1000 games. Exact build, device/Android or virtual environment, floor/seed, source of ignition, inventory, error and recovery state are unknown. Do not infer that fire caused the crash merely from its timing, or that floor layout was regenerated rather than progress being rolled back.

## Source findings

Reviewed current real branch head098353d29dd27339f807bcdee99bdab6079663a4 after fast-forwarding without overwriting newer settings work. Compared tagged254 source51bdf6b18b1abb3c861ee80186f67def72e17df5. `actors/blobs/Fire.java` is byte-identical between254 and current. `actors/buffs/Burning.java` differs only by three display-only forecast getters; its actual tick/damage/inventory-burning loop is unchanged. This is a bounded comparison, not proof that every downstream damage/rendering/memory path is unchanged or safe.

`GameScene.onPause()` calls `Dungeon.saveAll()` in both254 and current. `InterlevelScene` and other explicit checkpoints also save. There is no normal every-turn save call in the burning loop. An abrupt termination without a newer completed save can therefore restore an earlier checkpoint, potentially floor entry. This explains a possible rollback mechanism, NOT the cause of this crash or proof that the tester's save was uncorrupted.

Current file-write changes since254 in `FileUtils` concern excluding loot-transfer journals from generic temporary-file cleanup, not a confirmed fire fix. Current diagnostic export records lifecycle/input/memory and available prior-session/uncaught-error evidence. It was introduced after254. OS kills/native faults can still lack a captured Java exception. Existing later memory fixes must not be presented as the proven resolution of this report.

## Verification

Current source only:42 headless cases across8 suites passed, zero failures/errors/skips. These exercise attributed Burning ticks and RNG, fire contact/spread/save-reload attribution, monster fire callers, Inferno safety, failed-write/roundtrip save safety and diagnostics. They do NOT reproduce this unknown phone run, exercise Android fire-particle rendering, simulate all low-memory/native/process-death conditions, or establish a fix for254.

- `FileUtilsSaveSafetyTest`: 4 cases
- `com.shatteredpixel.shatteredpixeldungeon.apd.diagnostics.SupportDiagnosticsTest`: 6 cases
- `com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.AttributedBurningIntegrationTest`: 6 cases
- `com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.FireCallerProvenanceTest`: 5 cases
- `com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.FireDamageProvenanceTest`: 5 cases
- `com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.InfernoDamageProvenanceTest`: 6 cases
- `com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.InfernoSourceSafetyTest`: 5 cases
- `com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.MonsterFireCallerTest`: 5 cases

The first local test invocation did not run because the old temporary Gradle harness directory had been removed. A fresh isolated headless harness was created and the selected42 cases then passed. Log: /workspace/apd-254-fire-investigation-tests.log. No application source, save format, balance, signing or assets changed; no APK build/publication was triggered.

## Next evidence

Use the existing released Alpha-Testers358 APK for install-over testing (never uninstall/clear data).360 is a newer emulator-accepted candidate in the handoff, not a published release. If the crash recurs, export APD Settings -> Advanced -> Create bug report after reopening, before extended further play where possible. Obtain exact build/device/environment, floor and fire source; establish whether recovery rolled back actions or changed layout. A diagnostic report may contain the previous session or last uncaught error but cannot guarantee recovery of OS/native crashes. Keep the issue open until reproduced or supported by a trace; do not change fire rates/damage or saving policy speculatively.
