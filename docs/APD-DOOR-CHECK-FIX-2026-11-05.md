# Door-check eligibility correction — November 5, 2026

Owner report: TEST362 shows checkmarks on a bookshelf-lined room where those tiles cannot be hidden doors. Floor1 and Floor10 also must not suggest searching for generated hidden doors.

## Actual root causes

The old filter reused the renderer's broad wall category, which includes bookshelves, and never checked the candidate tile's own type. It treated arbitrary solid furniture as a door frame, and treated every non-stone tile as potential backing. It had no public floor/generator eligibility gate. Its old tests explicitly accepted bookshelf backing and exercised positive searched-wall screenshots on Floor1, so they failed to cover the owner's intended rule.

## Code correction

`DoorSearchEligibility` is the shared public-rule policy for marker eligibility, cell information, explicit wall-target selection and completed-check presentation. Only ordinary wall and its visually indistinguishable secret-door disguise can be candidates. Decorated walls use a distinct tile graphic and cannot be hidden-door candidates; they can remain structural side frames beside a real door. Main-dungeon floors1,10,15,20,25,26 do not offer door checks. Scripted prison/caves/city/halls boss maps, final/dead-end maps, vault and mines are excluded by generator type too. Mining concealed connections are painted as diggable stone, not searchable secret-door tiles. Floor5 remains eligible, preserving Rat King searching and its separate optional Quick Search.

Observed geometry must admit a doorway orientation: structural side frames and potentially open backing on opposite sides. Visible furniture, blocked backing, pillars, corners and map borders rule the tile out. Undiscovered neighbors remain unknown; their actual terrain, room identities/connections and whether a secret actually exists are never used to prune markers. The runtime rule does not scan hidden-door locations or count remaining secrets.

New bookshelves never receive saved door marks. Existing saved marks are filtered at rendering/inspection time without erasing historical search memory. Existing unsupported-floor marks likewise stay hidden after loading/revisiting. Normal Search, trap discovery, hunger, turns, RNG, generation, inventory, statistics and signing identity are unchanged. Explicit impossible door targets are refused without spending a turn; general Search still works normally.

## Verification

Local full suite:2495 tests441 suites,0 failures/errors/skips. Eighteen added regressions cover all terrain types, all excluded floors, branch/generator rules, legacy raw marks, corners/pillars/backing/frames, fog invariance, map/RNG immutability and ordinary turn costs. A real generation observer checks78 floors across three complete seeded dungeons: excluded floors contain no secret-door tiles, and every real generated secret door remains eligible under fully observed geometry. Normal valid wall faces, southern overhangs, side faces, saved colors and guaranteed-versus-uncertain search evidence remain covered.

Native fixtures retain all existing cases. They now assert zero marks on actual Floor1 with injected old saved marks, descend through the real transition to Floor2, verify an injected bookshelf mark is absent, then run the original positive stone/overhang/color/save/resume checks. Both new negative examples are captured. Native/Android compilation and APK acceptance are pending at this report's source checkpoint; no physical-phone reproduction or acceptance is claimed.

## Delivery boundary

TEST362 remains the last actually released pair and still contains this bug. Preserve its private release/direct assets and all prior source/owner features, including the latest exact-occurrence timeline check (72-case Android inventory). No new APK or build number has been requested by this source-only check. One intentional replacement pair may follow successful source verification; no duplicate builds for documents or test-only corrections. Never call a failed or untested candidate verified.

## Source acceptance completed

Sourcef7558b16d219641ca8a418d516a5601dabac0aa1 passed source-only42079861146/job127429778801:2495 core cases441 suites, zero failures/errors/skips, Android debug/release/instrumentation Java and desktop compilation. Both original362 bookshelf/doorless-floor regressions were reproduced against the original source and then passed unchanged against the corrected code. No source history was rewritten. One intentional replacement pair is requested next; native/device acceptance and publication are not claimed from source checks.

## One candidate pair — TEST363

Producing source4749caf7a8dc2a3634ac52fbadf084758fc63f37. Developer/native42080532920 (producer job127432082963 succeeded), Alpha-Testers42080533002, six-profile display42080533049. Developer compiled successfully after the full2495 core cases and Android debug/release/desktop checks; the application artifact is not yet accepted or privately released. Exact tester acceptance is requested against the matching player artifact without rebuilding. Original signing/app ID and TEST362 fallback preserved.

## Final decorated-wall audit; partial363 withheld

The remaining renderer/decorator audit established that WALL_DECO is visibly distinct from WALL/SECRET_DOOR, and region painters only decorate ordinary WALL, not SECRET_DOOR. The first candidate still admitted WALL_DECO. Candidate363 was therefore withheld and its unfinished verification jobs cancelled before publication. It is unverified and must not be delivered as the fix. No published assets were overwritten. Its completed Developer/player artifacts remain the immediate previous compiled history, while TEST362 stays the last verified released fallback.

Decorated tiles now never display marks, including legacy marks. They remain valid structural neighbors, preserving real adjacent door candidates. The added decorated-wall regression and all existing cases pass:2496 core tests441 suites, zero failures/errors/skips. Native fixtures now capture three negatives (real Floor1, saved bookshelf, saved decorated wall) before the retained normal Floor2 positive/color/save checks. One final corrected pair follows source acceptance; no application rebuild for document or fixture-only corrections.

## Final source acceptance before the replacement pair

Full2498 core cases441 suites pass, zero failures/errors/skips. Twenty-one added cases cover the complete terrain eligibility policy, including decorated walls as non-candidates but valid structural side frames, consecutive-door backing and immutable map-border backing. The real78-floor audit still preserves every generated secret door. The preceding2496 checkpoint passed Android debug/release/instrumentation Java and desktop compilation in42082418369. The final producer must repeat the required checks on this exact final source before assembling an APK. Ordinary searching, its RNG, turn/hunger costs, save data and all prior features remain unchanged. No further generation or gameplay changes are included.

## Final acceptance before private publication

Frozen application source1103a2e7caf72a094836a7cf6931468f3f800b36: Developer/native42083792753 and matching tester producer42083792729 succeeded. Full2498 core tests441 suites; all72 native cases passed in one invocation; all six display profiles42083792722 passed. Initial actual-tester42084291430 passed3 cases, but visual review found an achievement banner obscuring two negative captures. Instrumentation-only4b250549f waits for the normal banner expiry and asserts its absence. Recheck42086446743 passed3 cases against the exact same APK; all three unobstructed negative captures were reviewed. No new application build or version increment for this capture correction. API36 baseline eight visual captures also reviewed. Physical-phone acceptance remains pending. Private publication/download-back checks are the remaining delivery gate.

## Final verified publication

## TEST364 complete door-check correction — verified private release

Application source `1103a2e7caf72a094836a7cf6931468f3f800b36`; tag `apd-alpha-preview-364`; private release `435592879`. Both existing APK variants passed2498 core cases441 suites; full72-case Developer API34 native run42083792753 passed in one successful invocation; all six display profiles and three actual tester cases passed. Three negative captures verify real Floor1 legacy marks, bookshelf and decorated-wall marks absent; Floor2 valid wall/overhang/color/save checks retained. No physical-phone acceptance claim. Raw player statistics, generation/RNG/search turns/hunger, original app ID/key and all prior owner features preserved.

Published `2026-11-05T18:47:23Z`; actual download-back verification `2026-11-05T18:47:31.018085+00:00`; final response links prepared `2026-11-05T18:47:54.999672+00:00`. Sending is evidenced separately by this task final response, not by invocation/scheduler metadata. Partial363 stayed withheld/unverified; no363 release and no untested candidate delivered. TEST362 and all old assets retained as fallback.

Developer: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-364/APD-Developer-364.apk

Alpha-Testers: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-364/APD-Alpha-Testers-364.apk

Matching source: https://github.com/APD-Development/ShieldingShouldHaveWorked/releases/download/apd-alpha-preview-364/APD-Source-364.zip

Install over only; never uninstall or clear data. Exact receipt: APD-TEST364-RELEASE-DELIVERY-RECEIPT.json.

