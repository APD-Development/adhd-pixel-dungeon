# UIMSD reports (shareable export)

Reports are built from the same data as the UIMSD screens (`apd.report`); there is no second statistics system. A `Report` is plain data
(headings, notes, labelled values, tables) in which every value keeps its `CellText` state, and every table says where its rows come from.

## Format decision
- Renderers: **Markdown**, **plain text**, a self-contained **HTML page**, and **CSV** (tables). One model, four pure, deterministic renderers.
- **No native PDF.** Investigated: Android's `PdfDocument` exists only on Android (core also builds for desktop/iOS), would need a pagination
  and font/layout engine, and gives a worse result than a browser. The HTML page has print CSS: any browser prints it to PDF. Revisit only if
  a user-visible need appears.
- Getting it out: **Share as text** (system share sheet, `ApdFiles.Provider.shareText`, up to 100,000 characters) or **Save** through the same
  system file picker as the history archive (`chooseExport(name, mime, target)`), so no storage permission is needed.

## Reports
| Report | Builder | Where in UIMSD |
|---|---|---|
| Overall/lifetime summary | `lifetime` (the Home cards) | Home -> Report |
| Current filtered view / any table | `view` (the table as filtered, sorted, grouped; cut at 1000 rows and says so) | Options -> Share a report of this view |
| Individual run (details, items, enemies, how it ended) | `run` | run row -> Report for this run |
| Character / item / enemy / death statistics | `group` (the group's statistics sheet) | group row -> Report for this group |
| Comparison of two runs | `compareRuns` (with a difference column) | run row -> Compare with another run |
| Comparison of two groups (two characters, item kinds, enemies...) | `compareGroups` (the groups' statistics sheets side by side) | group row -> Compare with another group |
| Seed floors + recorded vs canonical | `seedFloors` | Options (in a seed floors view) -> Share the seed floors report |

## Provenance rules
- `?`, `>=N`, `~N`, `hidden`, legacy stay in the values in every format (cells come from `CellFormatter`); each renderer prints a legend for exactly the
  marks that appear (HTML also styles them).
- Every table carries RECORDED / CANONICAL / "RECORDED and CANONICAL rows: see Basis". Any canonical content adds a prominent notice to the report:
  canonical rows are not recorded history and not what the run would have met. Canonical cells carry `~`; recorded floors show only the log.
- Hidden information (true identities...) only for finished runs and only when the player has hidden information switched on; such reports carry a
  spoiler warning. A run that is still going never includes it. Reports never contain the install's personal data beyond what the player chose to share.
- CSV cells starting with `=`, `+`, `-`, `@` (non-numbers) are prefixed so a spreadsheet cannot read them as formulas; HTML is escaped.

## Not done on purpose
Final wording/layout (separate polish pass); native PDF; reports of live (unfinished) runs' hidden information; online upload.

## Synthetic data and unfinished runs (stability pass 2)
- A report built from the synthetic demo or tutorial sample data says "SYNTHETIC DEMO DATA ... NOT real play history" at the top of all four formats
  (CSV: first comment line). `Catalog.synthetic` is set from the demo marker file; `WndUimsdReport.show` sets `Report.synthetic`.
- Comparing runs when either is unfinished leaves out every hidden column for both, even if hidden information was asked for (regression-tested).
- `Catalog` map access is synchronized: seed floors are installed from a worker thread while the screen reads (reproduced as an
  ArrayIndexOutOfBounds in a stress test before the fix).
