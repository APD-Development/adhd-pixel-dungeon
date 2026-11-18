# APD441 source changes, November 11, 2026

Acceptance update: APD441 subsequently passed producing run [43026044936](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43026044936), all twelve native cases, the affected-image reviews and exact-source scan [43028854285](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/43028854285). Both frozen APKs and corresponding source were delivered after exact saved-file readback. See [the final delivery receipt](APD-441-DELIVERY-2026-11-11.json) and [current handoff](APD-SESSION-2026-11-11.md). The preparation notes below retain their original verification sequence; their then-pending gates are complete in those final records.

The resumed baseline is `293b5a0d5fed957c1d953b301fadf576a766611b` on `codex/apd-phone-fixes-2026-10-09`. APD439/440 original APK recovery is separate from this new source candidate. No claim is made that these changes are in those original APKs.

## Bounded correction and retained work

Production resource changes are limited to the ordinary Excess Charge description and one Counter Ability refund number. Its rank factors now state 0.67 / 1.34 / 2.01 per effective imbued-wand level, including temporary bonuses and whole-point rounding. It explains that an existing stronger Barrier is retained rather than added. The existing `Wand.wandUsed` calculation is unchanged.

The ordinary Counter Ability rank +2 description said 0.77 charges, while the existing weapon-ability completion handler refunds rank × 0.375, exactly 0.75 at +2. Its four-rank runtime regression and existing Feint detail already use the correct value. Only 0.77 becomes 0.75 in the normal property; the rounded +1/+3 values and all handlers remain unchanged. The native QoL case also opens the actual ordinary Counter Ability window and records its top and reachable final line without changing or opening a calculations panel.

`ExcessChargeHandlerRegressionTest.java` is recovered byte-for-byte from prior commit `a14cf151638c7863a07f533d3a3de423bbe136e8` (Git blob `481b1b036de405e25e2c86256423c2e1c03f082c`). Its four tests invoke the real handler and preserve charges, time, ownership boundaries and RNG.

Six new `BossPylonTargetingIntegrationTest` methods exercise actual `Hero.checkVisibleMobs` and `AttackIndicator` refresh paths: ranged versus melee eligibility, reachable active pylons, hidden/inactive exclusion, blocked projectile paths including mind vision, preserved manual targets and a vulnerable boss. No additional production targeting changes were needed.

The existing Android QoL case now opens the ordinary Excess Charge window, checks its actual text and reachable last line, and captures normal/normal-bottom/expanded/bottom images. This is test code only; the existing twelve-case native acceptance set remains mandatory.

## Verification and limitations

For the initial Excess Charge/targeting slice, freshly compiled current sources: 1,718 main Java files across core/SPD-classes/services and 491 core test files, using the verified prior offline kit only for external dependency JARs. No previous compiled game or test classes were used. All 463 current test-source classes containing JUnit tests were included. Full result: 3,190 tests, zero failures, zero errors, zero skipped cases. All 3,179 original APD439 cases remain included.

After the Counter Ability one-number correction, another fresh compilation of all 1,718 main files and 491 test files passed. The relevant three suites then passed all 519 tests with zero failures, errors or skips, including the actual four-rank Counter Ability refund handler. The final candidate still requires the full 3,190-test gate and fresh Android instrumentation compilation in its build workflow.

The first full local run recorded two missing-history-index errors and zero assertion failures. Its evidence was retained. The exact current Git tree index and 26 missing historical TSVs were recovered, all 902 current game assets were present, and a fresh recompilation/full run passed. No assertion or expected behavior was weakened.

Source-only CI run `43021746221` at `dd9cfb270fba156815adb43cd4fd382830ea78c6` independently passed all 3,190 core tests across 463 suites and Android debug/release/instrumentation plus desktop compilation. Its downloaded XML and source proof were rechecked. It assembled no APK. The subsequent Trap screenshot correction described below still requires fresh Android compilation and native execution in the APD441 workflow. A new APK must pass all twelve original native cases plus affected-image review before delivery. Signing identity, .indev application ID, saves, statistics, Cleric balance, and all previous builds remain unchanged.

Cursed Wand wording and other owner-decision items remain pending. This slice does not complete the whole description audit.

## Verification-path corrections before the first APD441 assembly

The first APD441 request, run `43021889179`, stopped at the recovery prerequisite before compilation or APK assembly. Original recovery `43021022953` retained successful APD439 cases/package and APD440 Developer evidence, while its APD440 Alpha emulator lost its connection during image transfer after a completed QoL case. The revised candidate requires corrective run `43024704967` at `bbb24627d7717b7fd0869c3a1e8b524d9ad856c3` to pass its exact verification, six Alpha cases and package gates. The failed original run remains part of the recorded history.

The shared native runner replaces overlapping background and whole-directory transfers with serialized incremental transfers, a stable device SHA-256 image inventory and per-case transfer receipts. Filename handling includes the nested-class and duplicate-image names omitted by the older best-effort collector. PNG acceptance now checks CRCs, screen dimensions, complete decoded row lengths and valid row filters with the standard library. These changes correct verification infrastructure; they do not change game code.

`TrapDiscoveryDeviceTest` reuses its existing reveal/scroll path before capturing the enabled state after a fresh process and the reopened disabled state. Existing preference assertions, physical touch behavior, three test methods and filenames remain intact. The previous pictures showed the top of the settings panel, so their checkbox states were supported by assertions rather than visible checkbox captures.

The existing compiled Android JSON ABI check is restored after compilation and after assembly. The source/history scan wrapper verifies the exact frozen source archive using the existing scanner and review rules. No application rebuild is permitted once an APD441 built-pair artifact exists. Final delivery still requires exact APK/source hashes, original signature, all native cases, affected-image review and direct saved-file download readback.
