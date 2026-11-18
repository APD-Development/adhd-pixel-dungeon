# Pylon timing investigation — October 16, 2026

## Finding
No doubled pylon turn rate was reproduced in the bounded headless scheduler fixtures. Do not halve animation or gameplay speed. This does not close the owner's physical-phone report or establish which enemy/room was observed on depth22.

APD TEST79 and current TEST240 both contain Pylon.java blob669b6a84bad0d3886015ca14d0eb436f267e7b62 and Actor.java blobe674164d945d2285d7042638cbb76f16d93888ca. Current upstream SPD commit57a4e06a4caf162446d1c28caa7983f0493fecf0 contains the identical Pylon blob and PylonSprite blobe5e653630b3b3e53607b2178d38282c76b39b699. Actor.java differs in an unused VaultLevel import, an ID-peek accessor, whitespace and a comment; scheduler processing and duplicate-registration logic match.

## Mechanism
An active pylon shocks its current adjacent direction, then the opposite direction under ordinary rules. Stronger Bosses uses offsets3and5 instead of the opposite cell. It advances one of eight directions and spends one actor tick. Inactive pylons spend one tick without rotating. The direction and alignment are saved.

A player action taking two actor turns allows two pylon actions under the normal scheduler. The tests demonstrate that relationship; they do not prove that Slow, armor, a weapon or another particular effect caused the reported encounter. No such player-state attribution is made.

## New retained evidence
Four tests execute actual Pylon.act and Actor.process code:
- Sixteen normal-cost input-clock actions produce exactly16pylon acts at0.5x,1xand2x visual playback speeds, even when the same pylon is registered twice.
- Sixteen two-turn input-clock actions produce32pylon acts at every playback speed.
- Sixteen acts verify every adjacent-cell hit under normal and Stronger Bosses patterns.
- Inactive and serialized/restored pylons retain direction and one-tick action cost.

The scheduler fixture drives real production scheduling with a minimal input-clock Actor, not a real player strategy. Rendering/audio are suppressed; combat and turn logic remain production code. Fixture state restores the original actor registries, clock, IDs, level, hero, challenges, scene and playback selection. No owner save or history is used.

All4new methods plus8existing playback methods passed locally (12unique,0failure/error/skip;0.152seconds). Local production was compiled from exact TEST226 files whose Pylon, Actor and sprite blobs match TEST240; complete current-source CI remains required and is queued separately. Core-only CI does not build another unchanged application APK. TEST240 remains the current CI-verified application candidate and TEST231 the last privately published fallback.

## Remaining investigation
Physical-phone reproduction, the original run's action costs/statuses and identification of the actual rotating actor remain unverified. The fixtures do not show the full DM300 avoidance strategy working on a phone, every complete boss phase, or a render-only defect. A source match is evidence against a local pylon-code regression, not proof that the user experienced no bug. No gameplay behavior, timing, slider limit, scoring, RNG algorithm, save format or inventory mechanics were changed.
