# UIMSD Home and Guide

Home is the landing page of `UimsdScene`; the tables (advanced view) are one tap away (`All tables`, or a card's button).

## Home
- `UimsdHome` (pure, `apd/query`) is built from the same `Catalog` and `QueryEngine`/`CellFormatter` as the tables: no separate stats.
  Cards: runs, characters, difficulty (only when there are at least two groups to compare, across every ruleset), gear, enemies, recent, records. Each line carries a CellText state, so `?`, `>=`, `~`, dimmed small
  samples show exactly as in the tables. Scope is current rules, finished runs; the `Rules` button widens to all rules.
- `UimsdHomePane` draws it (scrolling cards, link buttons); `showRoot(view)` opens a table so Back from it returns to Home.
- Hidden information is untouched: Home reads the same finished-only, disclosure-checked datasets.

## Guide and tour
- `UimsdGuide`: 16 topics (the newer ones - reports, seed floors and canonical data, difficulty - have no tour step and no Show me button), each other topic linked to a tour step. `WndUimsdGuide` lists them; `Show me` starts the tour at that step.
- `UimsdTutorial`: 15 steps (a reports step was added); each expects NEXT or a real action (view, drill, back, filter, sort, columns), detected by comparing
  model snapshots, so the player really does it on the real interface. Skip step / Previous / Exit tour always work.
- Data: the player's own runs (when there are at least 5), else a made-up 30-run sample generated into `apd-tutorial-sample`
  (marker file, `demo-` log ids). It is shown with a SAMPLE DATA banner and deleted when the tour ends or on next UIMSD open.
  The sample uses the demo isolation rules: never recorded, exported, imported or counted as real history.
- The first visit offers the tour once (`SPDSettings.apdUimsdGuideSeen`); the Guide is always available.
- Wording is placeholder: the final language/UX pass is a separate milestone.

## Tests
`UimsdHomeTourTest` (Home cards/links/empty, guide topics, tour progression, mid-tour start, sample isolation, panel geometry) and
the Home/sample timings in `UimsdDeviceTimingTest` (API 26 + 34).
