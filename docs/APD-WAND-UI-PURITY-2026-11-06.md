# Wand UI purity — source work, no APK

Owner delivery hold remains active. This work follows delivered369 and is not included in that APK. The overall mechanics audit remains unfinished.

## Reproduced problems

Three failing regressions in `/workspace/apd-wand-ui-readonly-before.log` showed inventory titles and shop eligibility clearing inactive curse infusion, and the upgrade badge consuming Wild Magic RNG merely by being drawn. Bundle comparisons detect the state change; a seeded next-draw comparison detects the RNG change.

## Implementation

Wand UI uses the existing read-only base/effective-level previews. A variable Wild Magic level is displayed as an inclusive range rather than choosing a cast roll. Scalar consumers fall back to the known permanent level when no single effective value exists. Unknown upgrade levels stay withheld.

`Item.previewValue()` provides a quote path; Wand overrides it without cleanup. Shop, buyback, floor-sale, Alchemize and armband steal quotes use it. Actual transactions retain `value()` and its original cleanup; actual stealing retains normal charging and RNG. Shared price arithmetic preserves integer truncation and curse discount. Charge-budget arithmetic is shared by actual and preview armband paths.

## Evidence and boundaries

The original three regressions pass after the fix. A further 416-case matrix covers all thirteen wand types, levels -1/0/3/6, known/unknown levels, cursed/uncursed state and active/inactive infusion with resin. Pure quotes equal real transaction values, preserve serialized state and RNG, and actual known-item transactions still clean up inactive infusion. Wild Magic range is checked against actual casts and hidden-level privacy.

Targeted tests: `/workspace/apd-wand-ui-matrix.log`. Initial full suite passed 2,871 tests across 446 suites with no failures/errors/skips. A further nine-context armband test then passed against the original charge-loop formula and real quote functions, preserving both serialized item and artifact state. Final full suite passed 2,872 tests / 446 suites, zero failures/errors/skips; raw XML archived at `/workspace/apd-wand-ui-verified-local-junit.zip`. Android/desktop compilation and native layout acceptance are separate. The wider badge may require visual adjustment on small displays; it is not phone-tested. No save format, drop rate, cast probability, action cost or upgrade calculation changes are intended. No APK is assembled or delivered.
