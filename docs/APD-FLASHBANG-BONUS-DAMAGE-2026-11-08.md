# Flashbang electricity bonus damage provenance — 2026-11-08

## Confirmed defect

The base Flashbang explosion delegates to `Bomb.explode`, which already scales its original roll once with the immutable fuse/throw creator before the existing armor roll. The separate 25% electricity roll then called `Char.damage` directly. At non-neutral Player, Enemy or Boss Damage settings that attributable secondary roll therefore stayed at 100%.

## Bounded correction

The already-generated electricity amount now passes through `Bomb.apdScaleExplosionDamage` immediately before the original `Electricity` damage call. The captured bomb creator, original `Electricity` source object, damage roll, stun, targets, geometry, timing and death handling are unchanged. The helper returns the original integer directly at 100%, so neutral behavior and RNG order are preserved. Unowned/environmental bombs retain the unattributed 100% origin. Saved bombs retain their actual stored creator.

## Regression scope

`FlashBangBombDamageProvenanceTest` exercises Player, Enemy and Boss creators at every 25% step from 25% through 500%; the base pre-armor hit and electricity hit are asserted separately. It also checks neutral 100%, unattributed/environmental behavior, stored Boss creator restore, original Bomb/Electricity source objects and the next RNG value. Three new tests bring the intended full inventory to 2,975 tests / 454 suites.

## Verified source-only result

Exact source `53305259ecdcb230afe9ed4e5a8831c38c8671e0` passed source-only run `42493942932` / job `128772035131` at `2026-11-08T15:19:27Z`: 2,975 tests / 454 suites, zero failures, errors or skips, plus Android debug/release/instrumentation Java and desktop compilation. Artifact `13621709727` (520,265 bytes; SHA-256 `8eff843d66079a34873120a819c0ca51230e10c697b2f05d07b289c0fdbc776a`) was independently downloaded and XML-parsed at `2026-11-08T15:22:40Z`; its exact test XML contains the three named regression cases and its summary binds the source commit.

This remains post-APD377 source only: no APK was assembled or phone-tested, and APD377 does not contain it. No APK, save schema, score, Custom Rules setting, signing identity, application ID, build request or publication changed.
