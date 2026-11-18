# Circular vault sentry timing correction — October 16, 2026

## Confirmed source cause
APD's circular Imp-vault sentry advances its scan every actor turn; modern upstream SPD advances it every two turns. This is an actual attack-cycle difference, not a visual framerate adjustment or duplicated scheduler call.

Upstream commit40f1fe754f9f31e39eacdd27a946fc3711e2e834 (September30,2026, v4.0.1 hazard-room adjustment) explicitly added sentry.afterScanCooldown=2 in VaultCircleRoom.paint. APD TEST79 and current production retained the earlier room blob4b5d37225c40b546935bdc8dbe12613f29c314f7 without that assignment. VaultSentry's default interval remains1 and the actor itself matches upstream. The room configuration, rather than the shared actor/sprite, therefore produces twice as many scan advances.

This establishes a concrete issue consistent with a rotating sentry encountered during the Imp quest. It does not prove that this was the particular actor in the owner's original encounter; there is no captured original save/screenshot identifying it.

## Bounded correction
New circular vault rooms explicitly use the upstream two-turn scan interval. Their existing APD scan-pattern choices, damage, geometry and RNG sequence remain unchanged. The upstream patch also removed a pattern and adjusted other rooms; those separate balance changes are deliberately not included.

For existing saved floors, the existing Room.onLevelLoad callback repairs only a plain VaultSentry at the known center of a VaultCircleRoom, and only when its old interval and scans-per-cooldown both equal1. Direction, current countdown, pattern arrays, position, map, loot, player inventory and other sentries remain untouched. The repair is idempotent and consumes no RNG. No save-format version or raw history rewrite is introduced.

The DM300 Pylon is a different actor. Its source and sprite match upstream. Earlier retained real-scheduler checks passed:16one-turn actions produce16pylon acts,16two-turn actions produce32, at every playback speed. Do not describe a pylon timing defect as fixed.

## Reproduction and regressions
Five retained VaultCircleTimingTest methods exercise actual room painting, actual VaultSentry.act and full VaultLevel bundle restoration. Against unchanged production, four fail at the expected one-versus-two interval/direction/legacy-repair assertions, with the unrelated-sentry control passing. Corrected local code passes all5. Combined with4pylon and8existing playback regressions,17unique methods passed in0.227seconds.

Generation checks cover128fixed seeds and all existing pattern widths; the cycle checks32ticks at each playback speed. Saved-floor tests retain phase/pattern/position; controls preserve sentries outside the room, explicitly different intervals and multi-scan configurations. RNG and repeated-repair checks preserve the next random output.

Initial fixture setup omitted Level's normally initialized collection fields, producing NPEs before any assertion. The fixture was corrected; those errors are not counted as reproductions. A first RNG probe included new room construction, which consumes normal StandardRoom RNG; room construction was moved outside the measurement. The final four original failures are the asserted timing/repair failures, not fixture errors.

## Verification boundary
Dedicated exact-source before/after core acceptance and the complete application gate are pending. Required corrected suite:1716unique core methods, debug assembly, complete history against the actual preceding TEST240 APK,33API34methods and42valid Android screenshots. No failed or untested build is called verified. Last privately published TEST231 remains fallback; TEST240 is the existing CI-verified application candidate.

The earlier core-only pylon run38893931161/source37a193e6c13a4e8ed289819192a7501b60c7a32d independently passed1711unique methods,zero failure/error/skip. Its results do not verify this subsequent production correction.

Physical-phone reproduction and original-owner-save acceptance remain unverified. No public release, paid service, scoring, slider-limit change, new Cleric restriction, raw-data deletion or automatic tester distribution is included.


## Completed source and staging evidence
Exact source b5d562ca37fff9ac628af3f4ade731e9fa7d83f1 passed full run38894980574:1716unique core methods, debug assembly, complete TEST240 predecessor history,33API34methods and42valid reviewed screenshots,zero failures/errors/skips. Exact-original acceptance38894980632 independently reproduced the four expected failures and one healthy control, then passed1716. Separate128-seed map/pattern/nextRNG parity digest6078e48df512d854ff5cc8d5778686e6547ad2c4315cc665667c0097311214da is unchanged; only interval1→2 differs.

Source-bound stage38895319271 reused the existingAPK, verified actual signing/applicationID/version/checksum directly against published231, and produced a validated7-pagePDF. APKsha99ca269dbebcd19266dbf4e27aca798252ed3d3b021f97b730915cbbea52b7f4. Fullartifact12043559007; evidence12043828065. See docs/APD-TEST241-VERIFICATION-2026-10-16.json. Staging is not publication; separate11:55PDF/noonAPK delivery remains pending. No physical-phone or original-owner-run avoidance-strategy verification is claimed.
