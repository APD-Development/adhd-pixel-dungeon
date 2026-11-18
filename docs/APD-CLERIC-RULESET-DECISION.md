# Cleric un-ban: the ruleset identity options (decision for the owner; nothing is changed)

## The situation in plain English
Every run APD records is stamped with a "ruleset id": a short code that stands for the exact rules the run was played under (the base game version, whether APD's changes are on, the exotic-item setting, challenges, and, today, the words `cleric=blocked`). UIMSD uses the id to answer "my current rules" questions and to warn when a table mixes runs from different rules.

Un-banning Cleric changes the rules text, because Cleric stops being "blocked". If nothing else is done, every run recorded after the un-ban gets a different id from every run before it, even Warrior, Mage, Rogue, Huntress and Duelist runs, whose gameplay did not change at all. Cleric runs did not exist before, so they have no history to protect.

## The three options (from docs/APD-HANDOFF.md)
**A. New id for everything (bump to "r2", drop the Cleric word).**
- Plain English: from the un-ban on, all runs of every class get a new id. Your old history stays, but it counts as "different rules".
- Consequence: "my current rules" would show only post-un-ban runs until the history is old enough; to see everything you must pick "All rules (mixed)", and tables will say MIXED RULESETS. Simple to build, honest, but it makes your existing statistics look like another game for no real reason.
- Risk: low to build, medium annoyance for you.

**B. Keep the old id for runs whose gameplay did not change; new id only for Cleric runs. (Recommended.)**
- Plain English: the five classes that were always playable keep one continuous history across the un-ban, because their rules truly are the same. Cleric runs get their own new id because they are a new thing. Old raw logs are never edited; UIMSD simply reads the old "blocked" text as equivalent to the new rules for those five classes.
- Consequence: nothing in your existing statistics changes or splits. Cost: a small translation step when reading old runs, plus tests with old-log fixtures. If it were ever wrong it can be fixed without touching any data, because the translation lives in the reader.
- Risk: low; it is the only option that tells the truth about both groups.

**C. Leave the word "blocked" in the rules text forever and only change the real rule.**
- Plain English: Cleric would be playable while the record still says Cleric is blocked.
- Consequence: the stored identity would be false. Rejected: it breaks the principle that a run's identity states the rules it was played under.

## Recommendation
Choose **B**. It keeps your history whole, labels Cleric runs honestly as their own rules, and never rewrites a log. The change itself is small: remove the block in the hero-select Start button, change the Ruleset text and id for Cleric runs, add the read-time translation for old ids, bump the row-cache version so the tables rebuild, and add tests with old-log fixtures.

## What the un-ban unlocks
The Cleric humor's first-Cleric-run warning, real Cleric run counts in UIMSD, and the evidence needed to judge Guiding Light before any balance work (docs/APD-CLERIC-AUDIT.md; no balance change is proposed here and none will be made without your approval).

## What I will do after you choose
I will not unban Cleric until you decide. When you reply with A, B or C, the change is a few hours of work, tested locally, behind a default-off switch so it can be reviewed before it reaches a build.
