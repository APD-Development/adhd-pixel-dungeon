# TEST196 — canonical NPC interaction recording candidate

Timestamp: 2026-10-15T06:35:47Z

## Reproduction

The first queued candidate, levers, is not a mechanic in the current 4.0 source: there is no lever class, terrain constant, English message key, or interaction path. No invented event was added.

The next approved candidate is a concrete omission. The central hero interaction path called `Char.interact`, but the recorder only emitted `npc.quest` when one of four quest state machines advanced. Repeated conversations, shopkeeper interaction, and other accepted NPC interactions therefore left no canonical observation.

## Candidate

- Add public fact kind `npc.interact` at telemetry 1.12.
- Record exactly once after an NPC accepts the central hero interaction.
- Store only the NPC name already shown to the player and the current floor.
- Keep `npc.quest` unchanged and separate.
- A rejected interaction produces no event.
- Recorder calls remain guarded and exception-isolated; gameplay result, turns and RNG are unchanged.

## Verification request

Pending hosted verification: 1,610 core tests, debug assembly, immediate-previous compiled TEST192 APK-history comparison, 27 API34 methods and 35 validated screenshots. This is application source, so no TEST192 artifact reuse claim applies after the candidate commit.

No save, permanent statistics, scoring, custom-rule, gameplay, identity, signing, delivery or physical-phone change. TEST192 remains the verified fallback until the complete gate passes; TEST123 remains owner-delivered/signature-compared.

## TEST196 result and TEST197 correction

TEST196 source `37da2dcd7ef3512402c2d8622be73e075d2278ba`, run [38693568938](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38693568938) (06:37:58Z–06:40:00Z), executed all 1,610 core tests: 1,607 passed and three integration contracts failed before APK compilation.

1. `PlayWiringTest` correctly required the new sixth `ApdPlay` call in `Hero.java` to be declared.
2. `ContractTests` correctly required a permanent telemetry 1.12 milestone in `apd_records_history.txt`.
3. The new regression invoked all of private `Hero.actInteract`; its pre-existing `ready()` UI work requires `AttackIndicator.instance`, which does not exist headless, so it failed before the NPC interaction.

No failure implicated the production recorder logic, but TEST196 is not accepted and produced no APK. Core artifact `11952904481`, ZIP SHA-256 `7a11eab5b211a9190a02fc28920f2bd1de1ca718d94cab7f65944539aa270f34`.

TEST197 updates both permanent contracts and extracts only the call-plus-record portion to a private helper used by the real central path. The regression invokes that helper headless, without bypassing the actual production recording logic. Re-run the same 1,610-core/debug/immediate-previous compiled TEST192 history/27 API34/35 PNG gate.
