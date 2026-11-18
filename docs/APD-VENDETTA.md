# The Developer's Personal Vendetta Against Cleric (APPROVED 2026-10-08; built, switched OFF)

Status: roadmap entry plus proposed architecture and sample lines. NOTHING here is built. The full line library and the popup runtime are NOT started until the owner approves this document. Lower priority than backend work and pending tests. Cleric balance and mechanics are untouched by this feature (see docs/APD-CLERIC-AUDIT.md for the separate balance audit).

## What already exists on paper (reviewed)
docs/APD-HANDOFF.md "CLERIC HUMOR" (1)-(4) and "UIMSD absurd-query easter egg" already record: a one-time first-Cleric-run warning; very rare slander announcements with long real-time cooldowns; effectively unlimited variety through templates x recorded context; a presentation-only difficulty gag; Cleric lore; never during combat/bosses/dialogue; a player switch; Adult/Clean wording; never fabricate statistics. This proposal keeps all of that and generalizes it into one engine.
Reconciliation to approve: the old text says "weeks or months apart"; the new request says "reasonably rare". Proposed default: at most one popup per ~3 real days of play, hard cap 1 per session, never in the first 20 minutes after install, player-adjustable (Off / Rare / Less rare).

## Architecture (all offline, no AI, no network)
Package `apd/humor` (pure Java core, no libGDX types, so it is unit-testable headless):
1. **Line packs**: plain text assets `assets/apd/humor/*.txt`, one line per record: `id | category | tier | requires | weight | clean text || adult text`. `id` is a stable string (e.g. `clr.death.0412`), never renumbered, never reused; deleting a line retires its id. Adding lines = appending to a file. A lint test rejects duplicate ids, missing Clean text, unknown placeholders, over-long text, and Adult-only words in the Clean column.
2. **Categories**: `ANY_CHARACTER` (context-free, the pool that works while playing Warrior/Mage/Rogue/Huntress/Duelist/future), `CLERIC_DEATH`, `CLERIC_VICTORY`, `CLERIC_FAILED_ASCENSION`, `BOSS`, `ABILITY`, `EQUIPMENT`, `HISTORY` (uses recorded totals), `FOURTH_WALL`, `MENU`, `POST_VICTORY`, plus `character` tag so future characters/developer jokes reuse the engine (`vendetta=cleric` today; other targets are just another pack tag).
3. **Context** (`HumorContext`): an immutable snapshot built ONLY from existing recorded facts (UIMSD/run log queries, current hero class, last run outcome). Each line declares `requires` predicates (e.g. `lastRun.hero=CLERIC & lastRun.died`). A line is eligible only if every predicate is true on real data. Numbers in text (`{clericRuns}`, `{guidingLightCasts}`) are placeholders filled from recorded data; if the datum is unavailable the line is ineligible. Pure jokes with invented numbers ("would have died 17 floors ago") are only allowed when flagged `joke=1` and phrased as hypothetical/obvious exaggeration; they are never shown as a recorded statistic. The lint rejects a joke line that uses a recorded-stat placeholder next to invented numbers.
4. **Seen ledger** (`SeenLedger`): a small local file in app prefs storage, separate from run logs and never uploaded: id -> (timesShown, lastShownAt, lastShownRunIndex). Survives restarts and app updates; a corrupt file degrades to "nothing seen yet" (never crashes, never blocks gameplay). Cleared only by the player (a reset row in APD Settings) or reinstall.
5. **Selection** (`HumorPicker`): filter by humor level and eligibility, then (a) prefer never-seen lines, weighted; (b) when none remain, take lines outside the recent window (last N shown or last M days), least-recently-seen first; (c) never the same category twice in a row; (d) a template family cooldown so the same joke shape does not cluster. Uses its OWN `java.util.Random` seeded from the wall clock, never `Random.Int` of the game, so game RNG is untouched.
6. **Scheduler** (`HumorScheduler`): decides WHEN. Rolls only at safe moments: title screen, run-end results/after the death or victory window is dismissed, victory screen, between-level transition after the new level is fully loaded and the hero is idle with no enemy in view, and the pause/inventory-closed state. Never: mid-turn, in combat, with a hostile visible, during boss fights, in dialogue or quest windows, while a prompt or tutorial is open, during save/load, or at the same moment as the UIMSD tenth-run prompt. Gated by the cooldown/session cap above and the player switch. The popup is a simple window shown from the render thread through the existing window stack; dismissing it is the only interaction. It consumes no turns, stores nothing in the game save, and calls no game RNG.
7. **Humor level**: `SPDSettings.apdAdultHumor()` (default Clean). Unknown eligibility = Clean; age is never inferred. Every line has a Clean text; the Adult text is optional.
8. **Fourth-wall lines** are just a category. A first-Cleric-run warning is a special one-time line id with its own ledger entry.
9. **Isolation guarantees** (tested): no `Random`/`Dungeon.hero` mutation, no game-time advance, identical game RNG state with the system on or off, no references to the humor package from game logic except one call at each safe-moment hook (guarded by a source-scan test in the style of PlayWiringTest/BackendFoundationsTest).
10. **Extensibility**: a new character, boss or ability gets lines by adding a pack file and a requires-predicate; no code change. Predicate vocabulary is a small registry, extended when UIMSD gains new facts.

## Tests to write after approval
Persistence across restart; never-seen-first and no repeat until exhausted, recent-window behaviour after exhaustion; eligibility (Cleric-only lines never appear while the last run was Warrior, placeholders require data); safe-moment table (every unsafe state returns "not now"); Adult/Clean restriction and Clean fallback when Adult text is absent; unknown age defaults to Clean; game-RNG and game-state equality with/without the system across a seeded run; lint of every pack (ids, lengths, placeholders); cooldown/session cap; ledger corruption recovery; source-scan that the humor package is not used outside the allowed hooks.

## Content plan (after approval)
Seed pack ~300 hand-written lines across the categories, then growth toward thousands by appending packs (batches of ~100, reviewed by the owner in plain text before they ship). Template shapes plus recorded context multiply variety; the ledger guarantees a long-time player keeps meeting unseen lines.

## SAMPLE DIALOGUE (for tone approval; Clean first, Adult variant where it differs)
Context-free (any character):
- `any.0001` "Just checking in. You're still not playing Cleric, right? Good. You've made at least one intelligent decision today."
- `any.0002` "This message has nothing to do with your current run. I just remembered Cleric exists, and now I'm upset."
- `any.0003` "Unrelated announcement: Cleric is still a class. I have had a few days to be okay with this. I am not okay with this."
- `any.0004` "Whatever you are playing right now, it is better than Cleric. That is not a balance statement. It is a feeling."
- `any.0005` "I would like to apologize to every other class for sharing a menu with Cleric." Adult: "...for sharing a fucking menu with Cleric."
- `any.0006` "Reminder: hydrate, stretch, and do not pick Cleric." Adult: "...and don't you dare pick fucking Cleric."
- `any.0007` "Nothing important happened. I just wanted you to know I'm still thinking about Cleric. Carry on."
Fourth wall:
- `fw.0001` "As the developer, I want it on record that I made this popup on purpose. Cleric did this to me."
- `fw.0002` "Yes, I spent real development time on a message about Cleric. No, I do not regret it. Yes, I need a hobby."
- `fw.0003` "If this popup is annoying, there is a switch in APD Settings. Cleric has no such switch, which is exactly the problem."
Hypothetical jokes (flagged as jokes, never presented as recorded):
- `joke.0001` "Fun fact: if Cleric were a person, it would have died 17 floors ago. Anyway, carry on." (joke: invented number is the punchline)
- `joke.0002` "Hypothetically, Cleric would have found the trap. Realistically, Cleric would have walked into it twice."
Contextual, recorded data only (placeholders):
- `hist.0001` requires clericRuns>=100: "You have started {clericRuns} Cleric runs. I fundamentally do not understand you."
- `hist.0002` requires clericWins>=50: "You have won {clericWins} Cleric runs. I am beginning to suspect the problem may be me. I still hate Cleric."
- `hist.0003` requires guidingLightCasts>=1000: "You have cast Guiding Light {guidingLightCasts} times. This has not improved my opinion of Cleric."
- `hist.0004` requires currentHero!=CLERIC & clericRuns>=1: "You are playing {currentHero}, and your records show {clericRuns} Cleric runs. I am choosing to believe those were accidents."
Cleric death / victory / failed ascension (requires the last run to be real and Cleric):
- `clr.death.0001` "Cleric died on depth {depth}. I would say I'm sorry, but that would be lying, and I don't lie about Cleric."
- `clr.death.0002` "Another Cleric run, another death. At least it was consistent."
- `clr.win.0001` "You won as Cleric. Fine. I will allow one moment of respect. That moment is over."
- `clr.win.0002` Adult: "You won as Cleric. Congratulations, you beautiful masochist."
- `clr.asc.0001` requires last run Cleric, reached the Amulet floor, died: "You got that far with Cleric and still did not ascend. I know that feeling. It took me 225 attempts. I have not recovered."
  (225 is the owner's own stated count, used only in the developer's voice; not a computed player stat.)
Boss / ability / equipment (requires the fact to be recorded):
- `boss.0001` requires boss killed this run & hero is not Cleric: "Congratulations on defeating {boss}. Cleric would have needed three tries and a nap."
- `abl.0001` requires hero is Cleric & a spell cast recorded this run: "{spell}. Sure. Great spell. Very Cleric of you."
- `eqp.0001` requires hero holds a Tome: "That is a Cleric's book. I would like to speak to whoever gave it to you."
Menu / post-victory:
- `menu.0001` "Welcome back. No Cleric was harmed in the making of this title screen. Several were mentioned."
- `post.0001` "You just won a run. This is the best time to remember that Cleric exists. You're welcome."
First Cleric run (one-time, from the owner's own text, polished): "You have chosen Cleric. I fucking hate Cleric. It took me 225 attempts to finally ascend with this fucking class. I'm not going to stop you from playing it because apparently some people enjoy suffering. Good luck. You'll need it." Clean: "You have chosen Cleric. I really, really dislike Cleric. It took me 225 attempts to finally ascend with this class. I'm not going to stop you, because apparently some people enjoy suffering. Good luck. You'll need it."

## Decisions needed from the owner
1. Approve the architecture and the default frequency (one per ~3 days of play, one per session, player-adjustable) versus the older "weeks or months".
2. Approve tone of the samples; mark any that should never ship.
3. Whether the difficulty-display gag (HANDOFF item 3) joins this engine (presentation only) or stays separate.
4. Whether the seed pack is written by me for your review, or you supply lines.

---
## ADDENDUM (owner request: massive long-term dialogue system; proposal only, still nothing built)
Lore review: Cleric's in-game identity is the divine spellcaster with the Holy Tome, spells learned/upgraded via talents, subclasses Priest and Paladin, armor abilities Ascended Form, Trinity and Power of Many (core/src/main/assets/messages/actors/actors.properties). That text is never edited. All mockery lives in separate humor packs shown as clearly labelled developer messages ("Developer note"), so the lore and descriptions stay intact and the joke is the disproportion between a legitimate class and the developer's grudge.

### Scaling variety (hundreds of hours of Cleric play)
1. **Authored lines** (thousands, appended over time) are the base.
2. **Composable grammar**: pack files may also hold fragments (`opener`, `insult`, `subject`, `closer`, `aside`) with their own ids; a generated line is a combination ("Opener + subject-specific insult + closer"). Each fragment is tagged with context predicates, so combinations stay valid. 60 openers x 150 insults x 60 closers is already more than half a million distinct lines. The ledger tracks the combination key (fragment ids), plus each fragment individually, so the same insult core is not reused until the pool of cores is exhausted.
3. **Context multipliers**: each recorded fact (depth, cause of death, boss, subclass Priest/Paladin, specific spell cast counts, armor ability, Tome level, repeat-attempt streak, attempt number since last Cleric win, time since last Cleric run) adds eligible lines. Lines for rare situations ("died to a rat on depth 1 as Cleric") are written once and surface when that situation actually happens.
4. **Content growth never invalidates history**: ids are permanent, so shipping 500 new lines means the player's next messages are drawn from the unseen ones first.
5. **Graceful exhaustion**: when every eligible id has been seen, order by oldest lastShown, skip those inside the recent window, and prefer combinations (grammar) that were never produced; if even those run out, repeat the least recently shown. Never an error, never a blank popup, never a freeze.

### Frequency (reasonable, adaptive)
- Delivery is capped by a budget, independent of how many lines exist: default max one popup per session and about one per 3 days of play, never within the first 20 minutes, never twice in a row at the same place (menu vs run end).
- Playing Cleric does not spam: Cleric-specific moments (run end, first spell milestone) may raise priority, not the budget. A "Cleric humor" setting (Off / Rare / Less rare) and a one-tap "stop these" on the popup itself.
- A low-key alternative surface for heavy Cleric players who WANT more: a "Developer's Grudge" page reachable from APD Settings (read-on-demand, same ledger, never interrupts). Proposed, optional.

### Safety and isolation (unchanged, restated)
Presentation only; own random source; no game save writes; no effect on achievements, rankings, UIMSD data, submissions or statistics; humor ledger is a separate local file excluded from exports and backend sync. Adult text is shown only when Adult humor is explicitly on; unknown eligibility = Clean; every line requires a Clean text.

### More sample lines (tone check)
Deaths: "Cleric died to a {killer}. The Tome was right there. Reading is hard." / "Depth {depth}. Not a record, not a disaster, just Cleric." / Adult: "Dead on depth {depth}. Cleric strikes again, you glorious idiot."
Repeated attempts (needs recorded Cleric streak): "That is {clericStreak} Cleric runs in a row. I made it to 225 before I stopped being able to feel joy." / "Run {clericRuns} as Cleric. I would say 'try a different class', but I know you won't."
Failed ascension: "So close to the Amulet. I have been there. It does not get better." 
Victory: "You ascended with Cleric. I will never forgive you for making it look possible."
Abilities: "Ascended Form: ten turns of looking radiant before dying anyway." (joke, no recorded number) / "Power of Many: you made a friend out of light. You should try making one out of people."
Equipment: "A Cleric with a {weapon}. Brave. Wrong, but brave."
Priest/Paladin: "Priest. Of course you picked Priest. Healing everyone but my opinion of this class."
Fourth wall: "I wrote the Cleric lore text myself, you know. Okay, I did not. But I have strong feelings about it." (NOTE: only if true; lore text is upstream SPD, so this exact line is REJECTED; shown as an example of what lint/review must catch: never claim authorship of upstream text.)
Any character: "You are playing {currentHero}. Somewhere, a Cleric is losing to a rat. I am at peace for a moment."

### Tests (as listed above, plus)
Combination-key non-repeat; fragment-level non-repeat before reuse; exhaustion order (oldest first, recent window respected, never blank); history preserved after adding new packs; ledger kept out of exports/sync/UIMSD; Clean/Adult per fragment and per combination (a generated line is Adult only if every part has Adult text and Adult is on).

---
## ADDENDUM 2 (owner: permanent expansion rules; proposal only, nothing built)
### Permanent creative direction: Cleric is the ONLY target
The earlier wording "extensible to other characters / other developer jokes" is WITHDRAWN. The engine is named and scoped for Cleric alone (`apd/humor/cleric`). No other class gets a developer-hatred system; other classes may get ordinary humor/personality through the normal text system, which is a separate thing. The "ANY_CHARACTER" category stays: it means Cleric complaints that may pop up while the player is using another class, not complaints about that class. A source-scan test fails the build if any pack line, tag or code path targets a non-Cleric hero.

### Growth with every update, without resetting anyone
- Packs are append-only; ids are permanent and never reused or renumbered (a lint compares the pack against the previous release's id list, committed as `core/src/test/resources/apd/cleric_complaint_ids.txt`, and fails if an id disappears or its meaning changes; removing a line retires the id, still counted as "heard" for players who saw it).
- Players keep their seen history across updates. Picking is "unseen first", so a player who exhausted everything meets the newly added lines first (new ids are by definition unseen; ties favor the newest `since` version).
- Each line carries `since=<APD version>`. The number added per update is entirely the owner's call; there is no quota and no test requiring one.

### Patch notes and changelogs
- In-game and repository changelogs keep two visibly separate parts: the real change list (accurate, never joked about inside the list) and an optional "Developer's grudge" footer line in a distinct style/label. Cleric jokes never replace, alter or sit inside a genuine change entry.
- Example footers (tone only, NOT real notes): "Fixed three crashes. Cleric remains a disappointment." / "Improved UIMSD performance. Cleric contributed nothing." / "Fixed 0 Cleric problems. Added 63 reasons to hate Cleric."
- Any NUMBER in a footer about complaints is generated from the real pack diff by a small build-time tool (`added = ids in this version minus ids in the previous list`), never typed by hand, so "Added 47 new Cleric complaints" can only be written when 47 were actually added. A test checks the footer's number against the id-list diff. Footers about other fixes only state what the real change list states.
- I will never invent completed changes for a joke.

### The official complaint counter
- Where: APD Settings > About / Developer (an Easter-egg row, "Developer's grudge"), not on the main menu, not in gameplay. Opening it never triggers a popup by itself.
- Shows exactly: "I'VE COMPLAINED ABOUT CLERIC X TIMES." and "YOU'VE HEARD ME COMPLAIN X TIMES."
- X (official) = number of unique ids in the installed packs flagged `complaint` (authored complaints). Grammar-generated combinations are not counted toward either number, only authored ids, so the totals are stable and countable. Retired ids still count toward "heard" for players who saw them but the official total counts live ids only, so heard <= total is enforced by only counting ids that exist in the installed pack (a retired id never makes heard exceed total).
- Y (player) = distinct complaint ids in the seen ledger, each counted once however many times it was shown. The ledger is never cleared by updates; only the player's explicit reset (or reinstall) clears it, with a confirmation.
- No spoilers: only the two numbers are shown, never lists, titles, categories or "x remaining by category". The official total is the same for every player.
- Not gameplay data: stored in the humor ledger, excluded from run logs, UIMSD, achievements, rankings, submissions, exports and sync.
- A rare discovery joke (shown at most once, the first time the screen is opened, a ledger entry of its own): e.g. "Yes, I counted. No, I will not be taking questions." Never repeated, never spammy; the counter screen itself adds no further popups.

### Additional sample lines
- Counter discovery (once): "You found the counter. It only goes up. Like my blood pressure whenever Cleric is mentioned."
- Counter discovery (alt): "Yes, I keep count. Somebody has to hold Cleric accountable."
- Footers: see above (generated numbers only).
- Any-class: "Whoever you are playing today, you are not Cleric. Good. That is the whole message."

### Tests added to the list
Id-list immutability against the previous release; new ids are served before old ones for exhausted players; counter totals equal live complaint ids in the installed packs; heard counts distinct ids only and never decreases across updates/restarts; retired ids never push heard above total; counter screen and discovery joke are one-time and gameplay-free; footer numbers equal the pack diff; source-scan that no non-Cleric target exists.

---
## IMPLEMENTATION STATUS (2026-10-08)
Built and tested locally, behind `ClericGrudge.ENABLED = false` (core/src/main/java/.../apd/humor/ClericGrudge.java). Nothing is visible in the app until the owner flips it, after reading `docs/APD-VENDETTA-LINES.md` and trying it on the phone.

Done: line-pack format with strict lint (`LinePack`); seen ledger in a sibling folder `apd-humor/` of the history folder, never exported (`Ledger`); unseen-first picker with recent-window avoidance and newest-unseen preference (`Picker`); frequency modes Off / Rare / Rarer, default Rarer, one surprise popup per session at most, 20-minute grace, minimum gap 3 or 10 days (`Frequency`, `ClericGrudge.popup`); contextual inline lines with a separate small chance (`inline`); one-time first-Cleric-run warning and counter discovery joke; counter (`Counter`: "I'VE COMPLAINED ABOUT CLERIC X TIMES." / "YOU'VE HEARD ME COMPLAIN X TIMES."); game side `windows/GrudgeHooks` (title screen and post-run results screen only, never during play), `windows/WndClericGrudge` (counter, popup frequency, contextual jokes, reset with confirmation) reachable from APD Settings ("About / Developer: the grudge", only while enabled); 305 lines in three packs.
Tests (apd/humor): lint rules, no-repeat/exhaustion/new-lines-first, persistence and damaged-file recovery, eligibility by recorded facts, safe-state table, frequency and session cap, Adult/Clean, one-time lines, counter semantics across updates, game-RNG isolation, package isolation (only java.*), allowed callers only, Cleric is the only target (every line must be about Cleric; other classes may be named, praised or used as comparisons).

Not yet done (waits on approval of the lines, or needs a design step):
1. The first-Cleric-run warning has its text and its once-only logic, but is not yet shown from the hero-select Start button (touches the run-start flow; do after the lines are approved).
2. History facts (clericRuns, clericWins, clericDeaths, clericStreak, guidingLightCasts) and boss/spell/weapon/armor-ability facts are not yet passed in; those lines stay ineligible until a UIMSD-backed fact provider supplies real recorded values.
3. Composable fragments (opener + insult + closer) are designed but not built; add after the authored lines are approved.
4. Changelog footer: `python3 -I tools/humor_review.py` prints the real count of complaints added since the committed id list (`core/src/test/resources/apd/cleric_complaint_ids.txt`). Update that list when a release ships; quote only the printed number.
5. The difficulty-display gag is separate and later.

---
## CONSOLIDATED APPROVAL (2026-10-08, supersedes earlier Cleric humor instructions where they conflict)
The owner approved the full system: a permanent signature feature, Cleric the ONLY target. Two separate content pools sharing one engine:
- **Cleric Hatred** (ids `any.`, `fw.`, `menu.`, `clr.`, `oth.`, `asc.`, `post.`, `boss.`, `abl.`, `eqp.`, `hist.`, `joke.`, `fmt.`, `first.`) and
- **Cleric Appreciation** (ids `app.`, flag `appreciation`): sarcastic compliments, fake apologies, false redemption arcs; never sincere, never a statement about the game being changed.

Built (all behind `ClericGrudge.ENABLED = false`, tested locally, nothing in any build until the owner flips it after reviewing the lines and a phone test):
- Pools: `Pool.HATRED` / `Pool.APPRECIATION` per line; one picker, one ledger, separate counters.
- Linked stories: `setup`, `after=<id>`, `nextsession` flags. A follow-up is eligible only when its prerequisite is really in the player's ledger (and, for `nextsession`, was seen before this session began); once possible it is favoured (x4) so the payoff arrives. Lint and loading enforce: no missing prerequisite, no loop, no cross-pool link, every `setup` has a follow-up; lines that could never be shown are dropped so the counters never promise them.
- Lint also refuses any line that claims a balance or patch change (buff, nerf, patch, hotfix, rebalance, "next update").
- Moments: MENU, LEVEL_CALM (a long quiet stretch on a non-boss floor: hero idle, nothing hostile in view, no window), BOSS_CLEARED (first quiet moment after a boss fell), RUN_END_DEATH/VICTORY (results screen). Popups in play are a window, shown only when quiet; nothing is shown mid-combat, mid-turn, on boss floors, or with a window open. At most one unsolicited popup per session, 20-minute grace, minimum gap 3 days (Rare) or 10 days (Rarer, default), Off available; contextual lines have their own small chance and a separate on/off.
- Counter window (APD Settings > About / Developer, visible only while enabled), four counters with the owner's labels: I'VE COMPLAINED ABOUT CLERIC X TIMES. / YOU'VE HEARD ME COMPLAIN X TIMES. / I'VE APPRECIATED CLERIC X TIMES. / YOU'VE WITNESSED X QUESTIONABLE COMPLIMENTS. Distinct ids only; never reset by updates; no listing of undiscovered lines. The humor ledger is a separate local file (`apd-humor/ledger.txt`, beside the history folder, never exported, never in UIMSD or run logs).
- Content pack version: each line carries `since=<APD version>`; `ClericGrudge.addedByVersion()` and `tools/humor_review.py` report counts per version and per pool, which is what a patch-note footer may quote.
- Initial library: 205 Hatred + 116 Appreciation lines (+3 counter-screen jokes), all in `docs/APD-VENDETTA-LINES.md` for editorial review. The first drafts had ~300 Hatred lines; the weakest were cut before anything shipped (ids are only permanent once a release ships).

Patch-note rules: real technical changes are listed accurately and separately; the Cleric commentary is an optional, clearly separate "Developer's grudge" footer. A number in a footer comes from `python3 -I tools/humor_review.py` (added since the committed `cleric_complaint_ids.txt`), never typed. Tone bank (NOT release notes): "Fixed three crashes. Cleric remains a disappointment." / "Added N Cleric complaints. Unfortunately, Cleric still exists." / "Improved UIMSD performance. Cleric contributed nothing." / "Fixed an issue where Cleric was accidentally complimented." / "After extensive consideration, I've decided to forgive Cleric. Never mind."

Not built (needs approval or a design step): sharing/leaderboards/online anything (not wanted without separate approval); optional humor-collection achievements; the first-Cleric-run warning at hero select (text and once-only logic exist); history facts clericStreak and guidingLightCasts, and spell/weapon/armor-ability facts (clericRuns, clericWins and clericDeaths now come from the real Runs history via apd/query/ClericHistory, read once per session on a background thread), so lines that need the missing ones stay ineligible for now; composable fragments; the difficulty-display gag.

Phone test (owner, after review): flip `ClericGrudge.ENABLED` (new build, and Actions minutes are frozen, so build locally or wait for authorization); open APD Settings > About / Developer: the grudge; check the four counters; set popups to Rare to see one on the title screen after 20 minutes; confirm nothing ever appears mid-fight, on a boss floor, or with a window open; confirm Off stops them.

NOTE (dependency): Cleric is still blocked at run start (HeroSelectScene -> WndClericRemoved) until the owner decides the ruleset-identity question in docs/APD-CLERIC-AUDIT.md. The first-Cleric-run warning belongs to that un-ban step (it replaces the blocking window), so it is not wired yet. All other humor works today with any class; the Cleric history counts are simply zero until Cleric runs exist.

---
## PERMANENT CREATIVE DIRECTION (owner, 2026-10-08; applies for APD's lifetime)
1. **First-person developer voice is mandatory.** Whenever the game speaks as its developer (Cleric complaints and appreciation, developer commentary, patch notes and changelogs, fourth-wall lines, developer statistics, official in-game developer messages) it says "I", "me", "my". Never "we", "us", "our". Ordinary fictional characters may use plural pronouns; the rule governs the developer's voice. Enforced for line packs by `LinePack` (PLURAL_VOICE) and `CreativeDirectionTest`; patch notes follow the same rule by hand.
2. **A permanent, parallel creative priority.** Every development cycle should consider more Cleric dialogue, sarcastic appreciation, hidden jokes, callbacks and fourth-wall breaks. It must NEVER become a bottleneck for gameplay, UIMSD, backend work, bug fixes, testing or releases, and no release is delayed solely because new Cleric jokes are not ready.
3. **The game eventually acknowledges its own running joke, in layers introduced organically across updates, never dumped into the first release.** Planned layers: I complain about Cleric; the game acknowledges how often I complain; I complain about the complaint counter; the game questions why I keep adding complaints; I complain about players discussing my complaints; the game makes fun of me for being unable to stop; patch notes reference earlier Cleric jokes; rare appreciation stories create fake redemption arcs that collapse. The engine already supports them (lines may use the grudge's own REAL totals: `{complaintsHeard}`, `{complaintsTotal}`, `{appreciationsHeard}`, `{appreciationsTotal}`; long-delayed callbacks via `after=<id>,delay=<days>`; multi-session stories; `weight` 1 for extremely rare lines). The first content version ships none of the self-aware layers (tested), so they can arrive later as new `since` versions.
4. **Keep it surprising.** The whole library is never listed in a menu (only counts). Occasional extremely rare callbacks, multi-part jokes and long-delayed punchlines. Everything is optional: popups can be reduced or turned off and the player loses nothing of the actual game.
5. **Community.** Do not fabricate community reactions, quote real players without permission, or imply community events that did not happen. `LinePack` refuses any line mentioning a community, forums, social media, streamers or what "players told me". Future community-inspired jokes need the owner's approval first; that check is then changed deliberately.
6. **Cleric is the exclusive target.** Other characters may have ordinary jokes and personality. No equivalent vendetta, and no mechanical punishment for choosing Cleric.
7. **Approval.** The systems themselves are approved; do not ask again. The owner still reviews the actual dialogue (docs/APD-VENDETTA-LINES.md) before anything becomes player-facing.

Overall vision: APD takes its mechanics and statistics seriously and does not take the developer's hatred of Cleric seriously. Whenever players think I have run out of things to say about Cleric, I prove them wrong.

## CLARIFICATION (owner, after review of batch 1): other classes may be mentioned
The rule is that Cleric is the ONLY target of the developer's ongoing hatred, not that other characters' names are forbidden. Jokes may compare Cleric unfavorably with Warrior, Mage, Rogue, Huntress, Duelist and future characters ("Even Warrior is embarrassed to share a character-selection screen with Cleric."); other characters can be mentioned, praised or used as comparisons, but never become independent targets. `LinePack` therefore no longer rejects other class names; it still requires every line to be about Cleric (a line only about another class is refused), and class conditions (`hero=WARRIOR`) may name any class code. Tests: `PackLintTest.clericIsTheOnlyTargetButOtherClassesMayBeNamedAndPraised`.
Appreciation is distinct from Hatred by design: false praise, backhanded compliments, fake forgiveness and delayed betrayals (including long-delayed punchlines with `after=<id>,delay=<days>`). Complaint counts are never invented in a line (only the verified placeholders `{complaintsHeard}` etc. may carry a count).

---
## THE GRUDGE PAGE AND LOCATION CONTROLS (implemented 2026-10-08; behind ClericGrudge.ENABLED = false)
Where: APD Settings > "About / Developer: The Grudge" (only present while the feature is switched on; nothing on the main screen). The page (`windows/WndClericGrudge`, scrolls on small screens) has: the four counters; the frequency button (Rarer by default, then Rare, then Off); five independent location switches (Game startup, Main menu, During dungeon runs, After a run, Statistics screens; defaults on, on, on, on, off); the shared Adult humor (18+) checkbox (default Clean; age is never inferred); and "Forget what I've heard" with confirmation.
Rules (`GrudgePrefs`, `ClericGrudge.popup`, tested in `GrudgeSettingsTest`):
- Frequency and locations are independent: frequency Off means nothing anywhere; every location off means nothing at any frequency.
- One random roll per session (after the 20-minute first-time grace and the minimum gap) decides whether the session gets a message at all, so switching on more locations changes where a message can appear, never how many sessions get one. At most one unsolicited message per session.
- Every popup carries "Turn off these messages": it sets the frequency to Off immediately and saves it. (The old separate "contextual jokes" switch is gone; every unsolicited message is the same kind and obeys the same settings.)
- Stories (Appreciation) are ordinary lines as far as settings go: a follow-up is never forced through a disabled location, a disabled frequency, or the master switch.
- Choosing a line does not use it up. The game calls `confirm()` only after the window is really up; only then does a line count as heard, a story move on, and the session's message be used up.
- Moments map to locations: STARTUP (first title screen of the process, after a longer settle and with no window open), MENU (later title screens), RUN (LEVEL_CALM, BOSS_CLEARED: a long quiet stretch, hero idle, nothing hostile in view, not a boss floor, no window), AFTER_RUN (results screen after death/win/ascension), STATS (UIMSD and the records screen opened on purpose).
- Existing settings migrate untouched: the frequency keeps its old key; the new location switches start at their defaults; the seen history file is never rewritten by the change.

---
## FACT SEMANTICS AND ACCURACY RULES (audit after batch 4)
A contextual line may only claim what its conditions guarantee; a fact that is not supplied keeps the line ineligible. What each fact means:
- Run-end moments (results screen): `runVerified=1` (the game confirmed a genuine, normally ended run: the recorded result agrees with the call, and a win has a living hero), `lastOutcome` (WON = ascended; DIED), `lastHero`/`lastHeroName`, `hero`/`heroName` (the same class), `depth`, `subclass`, `killer` (only when the cause was a creature). EVERY line that uses lastHero, lastOutcome or lastHeroName must require `runVerified=1` (lint). A demo run never reaches this path.
- In play (LEVEL_CALM, BOSS_CLEARED): `hero`, `heroName`, `depth`, `subclass`; `boss` is a boss the hero has JUST defeated and only exists at BOSS_CLEARED (lint: the boss fact is for the BOSS category, which is shown only at BOSS_CLEARED).
- Title, startup and statistics screens: only history facts (`clericRuns`, `clericWins`, `clericDeaths` from the recorded Runs history) and the grudge's own totals. No hero is known there.
- Not supplied yet, so lines using them stay silent: `spell` (a Cleric spell cast in the run), `armorAbility` (an armor ability used, by its name: Ascended Form, Trinity, Power of Many), `weapon`, `clericStreak`, `guidingLightCasts`. Ability lines that name an ability require that ability by name; lines that imply dying require `lastOutcome=DIED`.
- Lines about the class being played right now (the "You are playing X" comparisons) are in the PLAYING category and appear only in play.
- No line mentions the developer's personal information (lint: PERSONAL), calls him "the developer" (lint), uses "we/us/our" (lint), or claims a balance or patch change (lint). Adult wording must be more than the Clean text with profanity inserted (test, with a short allow-list where the swearing is the punchline).
- (batch 5 audit) `lastAscended` is 1 only when the run was a COMPLETED ascension (the hero climbed back to the surface); a win by leaving through the Amulet screen is a win but not an ascension, so a victory line that talks about ascending must require `lastAscended=1` (lint). `lastAmulet` is 1 when the Amulet had been taken (used by "failed ascension" lines). `subclass` is only supplied for a real subclass (never NONE), and any line naming Priest or Paladin must require that exact subclass (lint). `clericAvailable` is read from the ruleset's own statement (it says `cleric=blocked` while Cleric is blocked), so the future un-ban flips it automatically; any line that tells the player not to pick Cleric requires `clericAvailable=1` (lint) and stays silent until then. No line claims a personal record the game has not verified. Players who enjoy Cleric are never the target: the jokes are about Cleric, and the developer can congratulate a player and still be furious at Cleric.
- (batch 6 audit) A boss counts as defeated when its own score in `Statistics.bossScores` turns positive (each boss adds a large positive score only when it dies; before that there are only penalties); the name comes from that index (Goo, Tengu, DM-300, the Dwarf King, Yog-Dzewa), and a run that is loaded with a boss already down says nothing. (The first version polled a total that the game only computes at the end of a run, so BOSS_CLEARED could never fire; fixed and tested.) Facts the game side really supplies are listed in `GrudgeHooks.SUPPLIED`, a test checks that list against the source, and lines using any other fact can never be shown. Mechanic numbers are not stated in lines: Ascended Form's duration can be extended, so no line says "ten turns"; spell outcomes are given as the developer's opinion, not as telemetry; unsupported counts about the developer's own 225 attempts are not invented beyond the 225 itself. Death-by-creature lines (boss.0008, boss.0010) are death lines and need a verified death plus a real killer.
