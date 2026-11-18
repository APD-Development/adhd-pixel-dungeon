# TEST117 custom health, defense, spawn and hunger candidate

TEST116 source d6c3f34594317307b949642850c24afe4e38c9d0, verification 38384131886 succeeded: downloaded XML confirms 1339 unique core and 21 API34 instrumentation cases, no failures/errors/skips, 22 valid PNGs and debug APK artifact 11805380201. It is staged, not delivered; fresh signing/application-ID/checksum publication gates and physical-phone testing are not claimed.

This package adds five gameplay effects: Enemy HP, Enemy Spawn Frequency, Hunger Depletion, Player Defense and Enemy Defense. Together with TEST116's six effects, eleven of eighteen have source implementations. Native controls are not implemented, so none is claimed alpha-complete. UIMSD work remains paused as requested; original scores are unchanged.

Enemy health applies once during initial generation and later actor insertion, with an additive optional saved flag; dead enemies remain dead and bosses/allies are excluded until the separate boss-health work. Spawn frequency affects ordinary initial density and respawn interval without changing XP or drops. Custom oversaturated placement now terminates when legal cells are exhausted; the original neutral loop is preserved. A fixture with eight legal cells and requested triple density verifies termination, not an owner-run reproduction. Hunger scales passive depletion and negative energy costs while keeping food restoration separate.

Defense scales the existing complete virtual armor/buff roll exactly once at its consumers. Direct attacks, trap/bomb reductions, warrior abilities, Skeleton/Tengu blasts and Chalice reductions use the same wrapper; immunities, resistances and shields remain separate. Chalice preview endpoints use the same defense scaling. Raw virtual overrides remain intact to avoid double scaling inherited rolls.

Nine new regression methods cover all enemy-health values, actual mob save/load and insertion, hunger values, six-class combined floor generation, bounded placement, spawn/cooldown boundaries, defense/RNG parity and Chalice preview support. A combined host run of these nine, six existing custom combat, three custom upgrade generation and eighteen existing Chalice methods passed 36 unique tests in 9.841s. Two initial host invocations failed due to missing test/dependency classpaths; corrected the harness without weakening assertions. The generation suite includes all 60 scroll totals with and without Forbidden Runes.

Full required 1348 core, 21 API34, 22 PNG and debug checks are pending for this candidate. No TEST117 APK or delivery claim. Last delivered TEST112 is retained. Seven gameplay effects remain unfinished: Player/Enemy/Boss damage, Boss HP, Food Availability, Healing Effectiveness and Equipment Availability. Damage attribution must cover all approved offensive sources, including summons and delayed effects, exactly once; it is not silently limited to ordinary attacks. No score coefficients or unrelated mechanics changed.


Verification run38387716215 completed: actual downloaded1348core/21API34 XML, zero failures/errors/skips,22validPNG/debugAPK staged as11807073393. No private release/signing receipt/phone test claim.
