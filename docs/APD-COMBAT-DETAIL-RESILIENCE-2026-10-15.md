# APD malformed combat-detail resilience — 2026-10-15

## Reproduced defect

A readable public ATTACK, DAMAGE or KILL can be followed by a correctly framed but malformed hidden detail payload. The current row builder decodes the hidden detail before applying the public event. If detail decoding throws, the surrounding catch silently drops the readable public combat event too, producing an exact-looking enemy and per-event tables with missing attacks, damage, kills or heals.

## Bounded correction

Decode each optional hidden detail independently in both enemy aggregation and event-table construction. A malformed detail becomes unknown identity metadata, while the readable public combat amount/count is retained exactly. Increase the disposable row-cache builder version from 15 to 16 so existing derived rows rebuild from untouched raw history. No raw log, save, score, rules, gameplay, turn, RNG, lore, signing or application-ID format changes.

Retained regressions cover ATTACK, DAMAGE and KILL together plus HEAL, checks the public Rat row/counts, confirms hidden class remains unknown, and verifies raw bytes are unchanged. Existing cache-version assertions move to builder 16 without weakening rejection of older versions.

## Pending acceptance

The candidate must reproduce the two new methods as the only intended failures against unchanged parent `7ca81c536318d3748190bd592a5f44ff5d2e6490`, then pass 1,693 core tests. The exact application source must additionally pass debug assembly, complete immediate-previous produced TEST228 history, 33 API34 emulator methods and 42 valid screenshots before it is called verified. No publication or physical-phone claim is requested.
