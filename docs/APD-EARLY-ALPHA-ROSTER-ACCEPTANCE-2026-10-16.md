# APD early-alpha six-class Android acceptance — October 16, 2026

Status: TEST249 exact-APK follow-up gate; remote evidence pending.

## Scope

This instrumentation-only gate reuses the fully verified TEST249 APK from source `37a8f6d16cd497e13ed581e887ed9d4858fe97aa`, run `38914965700`, artifact `12051339058`, SHA-256 `dcda79a5c6c83b1ac88e5294b3b134c8e1483109929b3a8da5365e7eadc8536e`.

On API 34 it must start, save and continue one disposable run for each shipped class: Warrior, Mage, Rogue, Huntress, Duelist and Cleric. Each run uses the same non-neutral 18-value rules snapshot, difficulty 5 and all nine challenges. Before and after reload, the test requires usable gameplay controls and the exact run-owned rules, class, difficulty, challenges, HP and provisional scoring authority. It verifies scoring record version 2, experimental status, calculation version and the exact combined multiplier. Neutral next-run preferences are deliberately applied before continue so they cannot silently replace either the run identity or its frozen score multiplier.

The gate also reruns the existing headless combination, five-main-boss phase/provenance and boss-health acceptance classes against the exact TEST249 compile tree.

## Boundaries

This is not a new application build, APK candidate, release or tester distribution. The workflow installs the existing verified TEST249 APK and compiles only the updated instrumentation package against the same application tree. It is emulator evidence, not physical-phone evidence. It does not claim full playthrough completion, boss kills, audio acceptance, or resolution of the unconfirmed armor report.

TEST249 remains the CI-verified staged candidate. TEST231 remains the latest privately published fallback until a later publication is independently confirmed.
