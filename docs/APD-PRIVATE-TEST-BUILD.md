# Private test build (owner phone test)

The normal build has every test feature OFF. A private test build turns on, and only on, what the owner approved for the private APK:
Demo Unlock (the UIMSD unlock gate and its demo) and the Cleric un-ban (option B). The Cleric Grudge / Lore feature is POSTPONED until the public release and is off in every build (central switch `ApdFeatures.GRUDGE_LORE_IN_THIS_RELEASE = false`; all its code and lines are kept).
Switches live in `core/.../apd/ApdFeatures.java`; they are read from the asset `apd/features.txt`, which exists only in a test build.

## Build (any machine with the Android SDK; NOT possible in the Claude cloud sandbox, see below)
```
./gradlew :android:assembleDebug -PapdTestBuild -PapdBuild=<number>
```
`-PapdTestBuild` generates `apd/features.txt` (testBuild, grudge, demoUnlock, clericPlayable) into the APK's assets and appends `-TEST` to the version name. Without the flag the file is absent and nothing changes. Installed app shows "(PRIVATE TEST BUILD)" at the top of the Grudge page (Settings → APD → About / Developer: The Grudge).

## Why it was not built here
The Claude cloud sandbox cannot fetch the Android Gradle plugin or Android SDK platform: they come from dl.google.com, which the environment's network policy denies (Maven Central and the Gradle plugin portal are allowed). Owner options: (a) allow `dl.google.com` in the environment's Network access (then a local build works with no Actions minutes), (b) build on the owner's own computer with the command above, (c) authorize one Actions run (a workflow_dispatch-only workflow would be added first).

## What the Cleric un-ban does (and does not)
- Cleric can be picked and started only when `clericPlayable` is on. The black "removed" screen stays for the normal build.
- Runs played as Cleric record `cleric=playable` in their ruleset text, so they get their own ruleset id. Every other run keeps exactly the ruleset text and id it always had, so existing history is untouched and continuous; nothing is rewritten.
- No Cleric balance change. The earned "attempts" badge and attempt counter are untouched.
- The one-time first-Cleric-run warning (grudge on) appears when Cleric is chosen and never blocks starting.

## Phone checklist
1. Install; title still as approved; Settings → APD shows the Grudge button; its page says (PRIVATE TEST BUILD).
2. Hero select: choose Cleric → (first time) the warning, then the run starts; no black screen.
3. Finish/abandon a run: UIMSD shows Cleric runs under their own rules, older runs unchanged.
4. Demo Unlock: before 10 genuine runs UIMSD shows the locked message and the demo button; use it.
5. Grudge: toggle the five locations, frequency, "Turn off these messages"; counters on the page.
