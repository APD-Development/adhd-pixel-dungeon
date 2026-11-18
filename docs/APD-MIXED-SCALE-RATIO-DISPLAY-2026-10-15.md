# Mixed-scale ratio presentation correction — October 15, 2026

## Reproduction

A generic UIMSD RATIO divides raw stored values. That is correct and must remain unchanged. Its presentation previously multiplied that raw ratio by 100 directly and printed raw numerator/denominator sums. When the two numeric columns used different fixed-point formats, the percentage and both displayed parts were wrong. For example, raw 250 hundredths divided by raw 1000 thousandths displayed 25% (250/1000), even though the player-facing values are 2.50 / 1.0 = 250%.

## Bounded correction

CellFormatter now applies the numerator and denominator display scales only at the RATIO presentation boundary. RATE is unchanged. Same-scale ratios remain unchanged apart from fixed-point parts being shown in their declared units. QueryEngine raw values, aggregate sorting order, filters, source rows, history, caches, saves, scores, slider rules, turns, RNG and gameplay are unchanged.

A retained regression covers hundredths/thousandths in both directions, a same-scale control, long and compact text, and confirms the raw AggValue remains unchanged. This candidate requires the full 1,637-core/debug/immediate-previous TEST206-history/28-API34/38-PNG gate before it can be called verified. It must not be claimed as part of delivered TEST204 or compiled TEST206.


## CI result — TEST207 CI gate completed — 2026-10-15T15:53:04Z
Run [38762160592](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38762160592) for exact source `f0b0cf7708a8d6af63cdb69392b22ab9e3962717` succeeded. The complete core, debug assembly, and immediate-previous compiled TEST206-history steps passed. API34 started and finished 28 methods with no reported failure or skip. CI logged 38 screenshots, each 320×640.

Artifacts are core `11986616803` (767,862 bytes; GitHub digest `99465f5c3a257239586e58e13545d0f732645209bab642d492bb73aeda94bcb3`), APK `11986731648` (36,059,570 bytes; `9951c5d863d26788675334b57ee6c6c1b2ea9ec7fa795b6f04efe1aeb2ed2c26`), and Android `11988085278` (3,225,355 bytes; `2628f834219cef1fe9bd891025edb62d52d8fa540bde7645f03d220a79af4024`). The executor repeatedly received HTTP 502 while retrieving artifact bytes, so the expected 1,637 core total was not independently recounted from XML, the APK payload SHA-256 remains unmeasured here, and the contact sheet was not manually reviewed. This is a successful CI gate with independent artifact review still pending, not a release or physical-device acceptance. TEST204 remains the owner-delivered fallback; newer TEST208 registry work is outside this evidence.
