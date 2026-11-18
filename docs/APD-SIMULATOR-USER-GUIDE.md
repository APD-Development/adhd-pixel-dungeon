# Downloadable APD Warrior simulator

This is a developer test package for a **new, empty Android emulator on a computer**. It is not a separate phone app. Do not install these APKs over the APD you play: the simulator uses the development application ID and creates test runs. The driver refuses physical devices and existing saves/statistics, but a dedicated disposable emulator is still required.

## What it does

The bot plays the actual APD game as a Warrior on Standard difficulty, without challenges. It explores, fights, collects ordinary loot, opens ordinary containers, throws usable missiles, eats when starving, spends a fixed talent sequence, and equips better fully identified, uncursed gear within its strength. It uses a held Healing potion only when the audited maximum raw melee damage of the selected adjacent enemy could kill it.

An episode ends on death, the first observed normal Swarm Healing drop, or reaching floor 5. These are early-game episodes, **not full dungeon runs**. Rare encounters are recorded rather than excluded. Unsupported interactions and timeouts remain failed attempts; they do not become successful results.

The healing trigger is conservative: it does not predict exact damage after armor, misses, ranged attacks, damage over time, or multiple attackers. The bot does not yet solve quests, experiment with unknown consumables, or optimize retreat. Combat randomness is not fully seeded. Results describe this specific strategy, not every human player's behavior or proof that a potion prevented death. No gameplay balance changes are made by the simulator.

## Get the package

Open the private repository's Actions tab and select **APD downloadable Warrior simulator**. Its `apd-warrior-simulator-download` artifact appears only after the exact source's core checks and five real native episodes pass. A queued or failed delivery run is not a verified download. GitHub sign-in and repository access are required; artifacts expire after seven days.

The package includes the two compiled APKs, their checksum/source manifest, the runner, this guide, and actual baseline episode traces. The APKs were tested together; do not mix APKs from different sources. `PACKAGE-TOOLS.json` records separate binary/runner and tested report-tool source commits and checksums. The separate 100-seed batch may still be running when this package becomes available; its outcome is not implied by a five-episode baseline.

## Run on your computer

1. Install Android Studio and create a **new, empty API 34 Google APIs x86_64 emulator** with hardware acceleration and enough free disk space (at least 9 GiB before image setup). Use a Bash shell, Python 3, Android platform tools (`adb`), and `sha256sum`. Linux is the current verified CI host; Windows/macOS host setup is not independently tested.
2. Start only that disposable emulator. Keep phones disconnected from `adb`; check `adb devices` and select the intended emulator using `ANDROID_SERIAL` if needed.
3. Extract the artifact and open a terminal in the folder containing `simulator-binaries/` and `tools/`.
4. Set the source from its manifest, then choose the seed/count and run:

```bash
export APD_SIMULATION_SOURCE="$(python3 -c 'import json; print(json.load(open("simulator-binaries/manifest.json"))["sourceCommit"])')"
export APD_SIMULATION_COUNT=10
export APD_SIMULATION_SEED=1000000
bash tools/run-warrior-instrumentation.sh
```

Count may be 1–100. `APD_SIMULATION_ACTIONS` may be 1–5000 (default 1000). Alternatively set `APD_SIMULATION_SEEDS` to an explicit comma-separated list of distinct seeds whose length matches the count. The package's policy is fixed; arbitrary plain-English strategies are not yet supported.

The script checks source and binary checksums before installation. It installs both APKs, runs instrumentation, and writes `simulation-results.jsonl`, `simulation-instrumentation.txt`, and `simulator-logcat.txt`. It does not uninstall or clear data. A nonzero exit means investigate retained evidence; do not call the batch passed. To run another batch, create another empty emulator: the first batch intentionally leaves its test history intact.

## Reading results

Each row records the seed, profile, completed status, terminal outcome, HP/depth, encountered enemy classes, commands/action trace, and measured episode timing. `observedHealingBeforeNormalFlyHealing` means a held potion was actually consumed before the normal Swarm drop; it does not prove the potion was necessary for survival. Shielding replacements and bonus loot do not falsely count as normal Swarm Healing drops.

The 100-seed experiment summary retains failed attempts and rejects missing seeds or mixed source cohorts. Summed parallel episode durations are not developer active time or wall-clock downtime. Automated emulator evidence is separate from physical-phone testing.
