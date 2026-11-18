# TEST84 stair shortcut discoverability
TEST79 included StairNavigationTag and the Up/Down popup, but the preference default was false. Its native test explicitly enabled the shortcut, leaving default discoverability untested. This explains absence with an unset preference; the owner's saved preference and exact phone rendering were not inspected.

The staircase icon appears in the side-control stack above the hotbar (right by default, left with flipped tags). One tap opens Stairs Up / Stairs Down. In TEST79, enable APD Settings > Automatic Stair Navigation to show it.

TEST84 enables an unset preference by default, preserves explicit Off/On and adds directions when enabled in settings. The native dungeon test now removes only its isolated test preference instead of force-enabling it, checks the default, then uses real pointer events to open the shortcut. Existing two-direction, discovery, highlighting, interruption, turn cost, save/reload and screen-size/Cleric overlap checks remain.

Local Java compilation passed; 18 settings/navigation checks passed in 0.274 seconds. Initial local test invocation omitted the libGDX native jar and all 18 failed at setup; adding the existing native jar corrected the harness. No game changes were made to address that setup failure.
Full core/debug/API34 instrumentation and screenshot review pending. No verified TEST84 APK or physical-phone claim yet. No movement, generation, collision, capacity, save or statistics changes.

Chalice preview request: investigation and local implementation are in progress separately; not included in this stair-only candidate. Confirmed base bounds ceil(3+2.5*level^2), floor(7+3.5*level^2); actual prick applies Earthroot/Rock Armor, DR, incoming modifiers, antimagic and shields. Life Link reactions require explicit prediction limitation. No Chalice change is claimed delivered.

Actual TEST84 run38311207786 failed:1208 core,1207pass/1fail, at VisibilityPreferencesTest's old default-Off expectation. Android/APK build skipped. No TEST84 APK exists. The expectation is updated in85, retaining original tile visuals Off; Android fixture now removes the actual ShatteredPixelDungeon preference key. TEST84 remains a failed attempt, never an APK history attribution.
