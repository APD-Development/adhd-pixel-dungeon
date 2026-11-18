# UIMSD look and wording (player-facing polish)

The brief: SAME POWER + SAME DATA + MUCH CLEARER PRESENTATION. Nothing was removed; the query engine, provenance marks (`?` `>=` `~` hidden), hidden-information rules,
reports and seed floors are untouched. This pass changed presentation and wording only.

## Design approach
* **One style layer** (`ui/UimsdStyle`): type sizes (title 9, text 7, help 6, metadata 5) and colours (title gold, section heading muted gold with a rule, text white, help grey,
  current/on green, warning amber, error red). Value colours in tables (exact/at least/inferred/unknown/hidden/legacy) are separate on purpose: they carry meaning (`UimsdTable.color`).
* **One list dialog** (`WndUimsdList`): entries are section headings, notes, or items (label + grey description, `(current)` marker, `[x]` switches). Every picker uses it:
  filter steps, sort, columns, options, rules, group by, tables, notes, guide.
* **Dense tables stay dense.** Added: two-line headings (no more cut labels), banded rows (already), a marked selected row while its sheet is open, edge marks and a
  "swipe sideways: N more columns" line when a table is wider than the screen, the sort arrow, a one-line description of the table under its name.
* **Sheets** (row and group windows) are two columns: grey label, value in its knowledge colour, small note under it.
* **Markup for long text** (`windows/UimsdText`): `# ` heading, `! ` warning, `- ` bullet, `[exact]`/`[lower]`/`[inferred]`/`[unknown]`/`[hidden]`/`[legacy]` colour tags.

## Filter workflow
Three steps, each saying where you are: what to filter on (columns grouped by what they describe, with a type hint), how to compare (plain words from `Op.word`/`Op.hint`),
then the value. Values are chosen, not typed, wherever the data knows them: enumerations (Won/Died/Abandoned, difficulty 1-5), and text columns offer the values actually
present with counts (`UimsdModel.choicesFor`: most frequent first, hidden columns of unfinished runs never contribute) plus "Something else...". "is one of" is a tick list.
Numbers, dates and free text are typed. Editing a filter marks the current comparison and values.

## Wording
Player words: *rules* (never "ruleset"), *run/item/encounter/death* from the table's own noun (`DatasetDef.unitSingular/unitPlural`; "Instances" became "Items"), *recording version*
(never "telemetry"), *worked out* for inferred, *canonical = rebuilt from a seed*. Technical terms that must stay are defined in the Guide glossary. `UimsdWordingTest` lints all
player text (guide, tour, views, columns and their groups, operators, Home, status lines incl. the broadest table, reports) for internal vocabulary AND database jargon.

## Home
Empty Home is a first-run welcome: what UIMSD is, what will fill the page, the tour as the main button (sample data, never touching real history), the Guide below it.
With data, cards have an accent bar, a rule under the title, labels left and values right in their knowledge colour.

## Safety net
`UimsdScreensDeviceTest` (device tests, API 26 and 34) builds the first-run Home, the tour's sample Home and tables at seven steps, and every dialog including every filter step for
every column, on the real engine. A layout that cannot be built is a failed test, not a crash on a phone.
