# APD TEST71 — October12,2026

## Current candidate
This APK adds optional enhanced tile visibility and automatic navigation to discovered stairs. It retains TEST70's tested hotbar-settings removal, reconstructed build histories and missing-history-reference fix.
Exact application source, successful CI run, test XML counts, checksum, signing certificate and timing are populated by the delivery verification section. Pending checks are not called successful.

## Changes since the immediately preceding produced APK, TEST70
Enhanced Tile Visibility is a persistent APD setting, off by default. It increases SPD's existing native checker shading from the ordinary default to its stronger 20% shade, preserving terrain artwork, water, grass, doors, fog, lighting and tile geometry. Turning it off restores the original visual-grid preference. It reuses the existing grid atlas and tilemap; no additional texture, tile dimensions or rendering pass is introduced.
Automatic Stair Navigation is a separate persistent setting, off by default. Its compact native stair tag opens Up and Down choices. The tag participates in the existing HUD layout, including wrapping beside four Cleric buttons on short screens.
Only stairs already visited or mapped are destinations. Navigation uses the existing pathfinder with a safe known-terrain route, then the ordinary Hero movement action and ordinary movement costs. It never uses the floor-transition action.
New visible enemies, damage, roots, known route hazards, blocked steps and manual cancellation stop stair travel. A stopped route cannot be resumed as a stale last action. The path remains transient; reloading restores the saved position and inventory without restarting travel.
Selecting a known stair briefly places a gold target marker at its exact tile on SPD's effect layer. It is separate from combat warning markers and never changes explored/visible flags. Offscreen stairs remain valid destinations; the marker is clipped by the normal viewport and does not reveal adjacent unknown terrain or force a camera jump.
Added19 host regression cases, individually documented in the offline TEST71 history. Added one Android travel/visibility case and expanded the existing Cleric overlap check to include the stair tag.
The Android evidence gate now requires the original five history screenshots plus six gameplay screenshots: original and enhanced terrain at zoom2 and4, the staircase popup, and the exact highlighted destination.
Recorded private TEST70 staging, this report, the new verification request and the shared handoff. No news article is invented for this private build.

## Files changed
Gameplay integration: actors/hero/Hero.java; apd/navigation/StairNavigation.java.
Native controls and marker: ui/StairNavigationTag.java; effects/StairHighlight.java; scenes/GameScene.java.
Settings and rendering: SPDSettings.java; windows/APDSettingsPane.java; tiles/GridTileMap.java.
Regression coverage: apd/navigation/StairNavigationTest.java; apd/visibility/VisibilityPreferencesTest.java; Android PlayRunDeviceTest.java.
Evidence and release records: tools/run-codex-android-checks.sh; the private existing-artifact staging workflow; reports, handoff, reviewed next-build ledger and build request.
Existing HeroAction, pathfinder, dungeon generation, item capacity, loot, combat balance, save formats, application ID and signing key are unchanged.

## Tests and evidence
Local changed application classes compiled.
19 targeted host cases passed: both stair directions; discovered/mapped/undiscovered and offscreen stairs; wrong-direction fallback prevention; blocked, unsafe and unknown tiles; visible traps/pits/fire without secret-trap revelation; enemy refusal without a turn; normal Move rather than floor transition; manual cancellation; exact normal movement cost; damage cancellation; save restoration; unchanged RNG and terrain; default options; disk-backed preference reload; original-grid restoration.
Host fixtures were corrected to initialize their level containers, hero talents and headless UI/sprite state. Their initial fixture failures are not presented as discovered player bugs.
The full required core suite, Android assembly, compiled-history equivalence and API34 instrumentation are reported from actual successful CI evidence. Expected counts are not substituted for test XML.
Android checks cover real pointer input on the stair tag, popup creation, blocked-route refusal, damage and newly visible enemy interruptions, ordinary walking to a selected destination, arrival without transition, saved pending travel/reload, original/enhanced visual comparison at two zooms, unchanged map/exploration/fog arrays, and HUD/quickslot compatibility. The travel setup uses a controlled test-only adjacent transition on the emulator's own run.
Native screenshots are reviewed before publication. No physical-phone or Android17/Pixel Buds acceptance testing is claimed.

## Historical reconstruction retained
The complete historical census inspected132 retained APKs across Legacy Builds1–84, Codex TEST1–67 and Private TEST1–3. It covered154 numbered records,1,902 source changes and2,859 individual entries.
All132 retained historical APKs have complete source-backed histories; zero remain partial or unavailable within that audited scope. These totals do not claim every historical APK passed gameplay verification.
21 historical numbers have no retained APK. Legacy:1,31,38,47,79,83. Codex:4,12,13,14,15,16,19,28,30,38,52,53,56,57,59.
Private TEST2 is a re-delivery of TEST1, explicitly identified as an alias.
Newer TEST68 and TEST70 have their own produced-APK records; TEST69 failed before producing an APK. TEST71 is separately attributed to the immediately preceding produced APK70. Numbering namespaces remain separate.
Historical limits: evidence includes actual artifacts, package/version, checksum, build source, source comparisons and bundled history; the audit did not formally decompile and prove every historical method.
History screens were verified on API34 in TEST70: compact native two-column categories, grouped horizontal icons, gold borders, short build-specific popups and bounded build navigation. All275 Build22 entries remain available, with no arbitrary cap.

## Unsuccessful preparations
TEST68 source53276e8706f23017b23eb624643d67f60c0f4603, run38259755048:1,160 core tests passed; assembly and compiled-history checks passed;20 Android tests ran with19 passing and one failing. An obsolete private-test identifier pointed to a missing history file; the SPD-reference PNG was therefore not captured and the image gate also failed. It was not delivered as playable.
TEST69 source88e73db126152e3d779603cd989473910de972e9, run38261032941: stopped before assembly because TEST68 had failed overall verification. No APK was produced.
TEST70 source445c47fb1fe21b86055260fe3f34468cc22c51e1, run38262103238:1,160 core tests and20 Android tests passed, with zero failures/errors/skips. All five screenshots were inspected. The obsolete private aliases were correctly replaced by reviewed canonical records, with permanent missing-reference tests.
TEST70 staging run38264066998 verified signatures and application identity against TEST67. APK SHA256:bb759ddd52fc6076c50e2d9b63500ebb46afbe5c63692ed8ffd7f0e433d7f62e. Certificate:067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e. It was privately staged, not published as the new feature build.

## Risks and remaining limits
The tile change strengthens native ground contrast; it does not replace all terrain artwork. Visual preference remains subjective and needs owner/phone feedback.
The highlight marks an exact known tile even under fog, without revealing surrounding terrain; offscreen markers are not forcibly brought into view.
Automatic stair travel is convenience navigation, not a survival guarantee. Unknown hazards retain ordinary exploration risk. Only known regular/surface stair directions are selected; no hidden branch destination is inferred.
Game saves and raw statistics are preserved. The original TEST50 player's run was not supplied or reproduced; no proven backpack-capacity bug is claimed.
The full UIMSD audit and broader Community work remain unfinished. No online system, paid service, public release or unattended coding mechanism is activated.

## Timing and delivery
TEST70 CI ran14:19:47–14:32:03 UTC,10:19:47–10:32:03 EDT.
The owner extended the deadline to noon EDT on October12. Current CI/staging/publication timestamps come from actual evidence. No unsupported overnight execution hours are claimed.
Install the signed-compatible private APK over the existing APD app. Never uninstall or clear player data. Preserve the last verified APK if the current candidate fails any gate.
