# TEST192 streaming run-log file reader — 2026-10-15

Status: hosted/emulator verification complete; no delivery or physical-phone claim.

Current production `RunLogReader.read(File)` first copied the complete log into a byte array, even though the format already caps each frame payload at 16 MiB. A controlled 1 GiB sparse file with a bad byte immediately after a valid header therefore made memory cost follow the file's damaged tail rather than the valid prefix. This is a bounded source/API robustness finding, not an observed phone crash or claim about normal player log sizes.

The correction streams ordinary file reads one checksum-validated frame at a time. It keeps the existing byte-array parser for explicit resynchronization, whose scan-ahead behavior requires random access. Header and per-frame allocations remain format-bounded; decoded events and strings are still retained exactly as before. A parser-parity regression checks a valid sealed log, and a sparse-tail regression requires the reader to stop at the first bad byte while reporting the full file length and damaged-byte count.

No log format, event meaning, raw history, save, permanent statistics, scoring, gameplay, application ID, signing or release behavior changes.

Verification run [38682901119](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38682901119) ran 2026-10-15T04:42:48Z–04:55:53Z and passed 1,608 core tests in 311 XML suites plus 27 API34 tests, all with zero failures/errors/skips. Debug assembly and immediate-previous compiled TEST191 history passed. All 35 expected screenshots are valid 320×640 PNGs and the full contact sheet reviewed clean. APK SHA-256: `129626b1bd0a65453c207c9fa5d06075a048b2e4d63e30ee034d0e1b15ca846f`.

Artifacts: core `11949475185` / ZIP `08b08aff0bc224fa0ade8d9097985f62e4b882bd82c3705a313313e72f544f12`; APK `11949125498` / ZIP `250c7d0329d61cea59e3db995dce5d3dcd936eba744e634b86fa8fd0393f9495`; Android `11949591656` / ZIP `7c401ca09f78fa0111d27b7472271029c916a91a53e27ca5005eb6559a7b4869`. No delivery or physical-phone claim; TEST123 remains owner-delivered/signature-compared.
