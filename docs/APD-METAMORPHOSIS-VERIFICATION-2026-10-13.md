# TEST107 deterministic class-position metamorphosis

## Approved feature implemented

Scroll of Metamorphosis no longer randomly picks a talent from each other class. For each eligible class-talent slot it offers the same tier/position from all five classes other than the class being played. Existing native TalentTierPane icons and WndInfoTalent popups are reused. The detail identifies the source class and position; all five class alternatives remain visible.

SPD already excludes talents currently owned from valid replacement choices. That protection remains: already-owned alternatives are shown as unavailable, with a disabled confirmation and an explanation. No different-position alternative or duplicate talent is silently granted. A second guard at application time prevents point loss from stale/invalid/duplicate selections. The earlier duplicate-choice question asked about presentation; this implementation preserves the existing valid-selection rule rather than inferring permission to change it. If all five are already owned in an unusual previously modified row, all remain unavailable; no new refund/cancellation rule is invented.

Original consumption timing, identified/unidentified behavior, cancellation warning, normal turn cost, transferred points and alias simplification are preserved. The old map/alias replacement code moves from the button callback into a small testable helper. Existing save fields are reused; no new game-save format, random-selection fallback, class restriction, stat scaling or unrelated gameplay changes. Eligible class positions retain SPD's existing tier/subclass access rules.

## Verification

Seven new regression methods pass0.097 seconds on the host JVM. They cover every class and eligible base tier/position, all five alternatives, transferred points/order, old random-save duplicate states, all-five-owned rows, repeated alias simplification, existing talent Bundle save/load, invalid/stale selections and no gameplay RNG draw in candidate calculation/application. These looped scenarios are sevenJUnit cases, not hundreds of extra counted tests. All changed production Java classes compile locally.

Existing Android play/save/continue test gains real pointer interaction through the talent-choice popup and confirmation, checks five distinct other classes and icon bounds, verifies the source-class detail and normal single scroll consumption, then checks the actual replacement after game save/continue. Two images are collected: metamorph-five-choices.png and metamorph-choice-detail.png. The existing20 images remain required, giving22. Full1300core/21API34/22PNG/debug checks and Android compilation are pending at source commit. No physical-phone/owner-save evidence or private TEST107 delivery is claimed yet.

Potential regressions: tier-position mapping for old modified rows, duplicate protection, source-class text layout, modal touch routing or alias/save restoration. Unit guards cover mapping and data; the new native interaction/capture must pass before delivery. Existing talent information callbacks keep enabled=true/empty extra text defaults.

