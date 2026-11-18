# TEST90: preserve empty-string saved filters

Source parent 6004e9581222f35290e2da8fb256c9c193b611cc; previous actually produced and delivered TEST89 application source b529832d07a1a2ba30453427edbd346dbf8b4124/run38324730902.

## Confirmed failure
QueryCodec encoded zero text operands and one empty-string operand identically. Decode therefore lost the empty-string operand. Reproduced through the codec API; the player's UI route for constructing such a filter has not been reproduced. This is a saved-query integrity correction, not a claimed player-reported gameplay bug.

## Correction and compatibility
Only the ambiguous single-empty-string filter receives an optional text-count field. Ordinary filters retain their prior encoding. Readers still accept legacy records with no count and retain their existing meaning; old ambiguous records cannot be distinguished retroactively and are not rewritten. Multiple empty values, pipes, tabs, newlines, backslashes and Unicode round-trip correctly. Invalid explicit counts are rejected rather than silently altering the query.
The existing SavedViews container, raw logs, game saves, statistics calculations, rulesets and limits are untouched. Existing opaque stored view strings remain preserved. Older applications still have their original inability to read a single-empty operand accurately; no downgrade compatibility for that previously broken case is claimed.

## Evidence
One baseline reproduction failed: expected one empty string, decoded zero strings. Five new permanent cases cover singleton-empty roundtrip, zero/multiple/escaped values, old records and ordinary encoding, malformed count rejection, and actual SavedViews store/load.
Compiled the actual current SavedViews production/test source with the fix. New tests plus eleven existing SavedViews tests and eighteen query-engine tests:34unique tests,0failures,0.160seconds.
Initial compile omitted an existing QuerySupport test dependency; correcting the local classpath resolved that fixture setup failure. No tests were weakened.
Full expected1244core+21API34+18PNG and debug verification are requested and pending at this source commit. TEST89 remains the last verified delivered APK. This fix is not in TEST89.

## Remaining work
Complete UIMSD audit and visual overhaul, historical gaps, simulator/custom-rules foundations, exact-owner-run bugs and physical-phone acceptance remain unfinished. Live community services and unapproved scoring/design decisions remain outside implementation authorization. No promise of uninterrupted background coding or entire-backlog completion is made.
