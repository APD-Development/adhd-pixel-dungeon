Confirmed one-decimal mean/median display saturation, locally fixed; not committed or delivered.

Actual QueryEngine mean/median of a valid Long.MAX_VALUE or Long.MIN_VALUE observation produced a finite double value. CellFormatter.oneDecimal multiplied by10 then Math.round saturated, shrinking the displayed result by approximately10times. Baseline failed 1of2 new cases in0.082s (first positive mean assertion); ordinary-value case passed.

Local fix adds an out-of-scaled-long-range path using BigDecimal one-decimal formatting and trailing-zero removal. Ordinary Math.round behavior stays unchanged, including negative-half rounding. The represented double value is retained; existing query-engine floating-point precision is NOT recovered or claimed exact.

2new cases cover large means/medians in both signs and unchanged ordinary positive/negative/half rounding. Freshly compiled new cases plus existing CellFormatterTest, FormatterBoundaryTest and AggregateExtensionsTest:26unique passes0.143s. Initial run lacked one compiled fixture class; supplied current source and reran without assertion changes.

Existing Android screen test strengthened to open actual large mean/median Question answer windows and check labels/text bounds. Android/full compilation pending; no nextAPK claim. Raw logs, save formats, actual aggregate calculations, query limits and gameplay are unchanged.

Remaining broader issues: SUM display and ratio reference counts can also exceed long display metadata; full precision decisions and supported representations need further audit. Whole UIMSD audit remains incomplete.
