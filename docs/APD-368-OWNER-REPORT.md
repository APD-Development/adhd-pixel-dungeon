# APD368 — statistics memory fixes and readable calculations

This candidate includes the reproduced UIMSD memory fixes and the consolidated Search, waiting, automatic eating and mechanics-description work. It is a private test update, not a claim that every description or physical-phone scenario has been verified.

## Changes and causes

The physical build365 crash exhausted its256MiB Java heap while reopening UIMSD. Foreground opening could overlap title preparation and retain a previous catalog. Cache error handling could retry raw parsing after memory exhaustion; an optional worker could also let a fatal error escape. The update transfers or retires preload ownership, releases previous derived tables, avoids raw retry after memory failure, handles optional-worker errors, and excludes simultaneous background catalog working allocations. Raw history and saves are preserved. Exact allocation proportions cannot be established without a heap dump.

Expanded calculation panels explain equipment, glyphs, enchantments, rings, talents, buffs, abilities, projectiles, trinkets, potions, scrolls, bombs and resource effects using actual source rules. Default text remains separate from expandable details. Inspection tests protect random state, hidden properties and live effects. Corrections include the Might elixir's level-based base-HP preview, Lunar Pasty nutrition, Rot Dart's five-use durability preview, and description paths that could activate emergency Berserk or consume a pending dart position. Damage, hunger, discovery probabilities and balance calculations remain unchanged.

The earlier Single-tap Search option, saved wall-search progress/markers, configurable waiting stops and optional automatic eating remain included. Existing origin/range tracking, both loot-gathering scrolls, healing controls, conversion rules, throwable targeting and Alpha playback choices remain preserved.

## Known limits and follow-up

The whole-game semantic audit and physical-phone acceptance remain open. Newly added raw-meat and Poison Dart details call their Poison value strength; the actual source parameter is duration and should be labelled in game-time units. This is an explanation defect, not a change to Poison's calculation. This test update must not be called completion of every requested explanation. No further application pair is requested today; exact existing APKs are reused for instrumentation or delivery work.

Source:7ef0800dfd6c4e624d3c001e886eb1a711d80a68. Developer producer42262060915; Alpha42262060908; display42262060874; exact Alpha acceptance42262576345; exact panel acceptance42262761458. Publication, asset download verification and owner-link delivery are distinct events.

## Verification

Preparation only: both variants' downloaded core XML contains2835 tests in446 suites, zero failures/errors/skips. The local constrained256MiB suite passed92 tests in12 suites. Full native, display, signature and exact-APK acceptance receipts are still being collected; their successful results must be established before publication. Developer SHA2568e23c1cb147a536212142a3f23d923773f02a9857e47a28a5d64a874c016ed12; Alpha SHA256f3228dd0259848d756a88d063fa11a84176ac483c7dd2bd97b4d7706362f1fd8. Both downloaded sidecars matched; common assets are byte-identical and Alpha contains only extendedPlayback in its feature manifest. Physical-phone testing has not been performed by Codex.
