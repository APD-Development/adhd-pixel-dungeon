# TEST227 Cleric collection-ledger preservation candidate

Recorded: 2026-10-16T00:48:52Z (2026-10-15 8:48:52 p.m. EDT)

## Reproduction

The delivered TEST226 implementation tolerated malformed ledger rows in memory, but did not retain knowledge that loading was incomplete. A later automatic save serialized only recognized rows and permanently discarded every unknown original byte. Its rename fallback also deleted the destination before retrying, so an empty directory at the configured ledger path could be replaced by a regular file.

Two permanent JUnit methods reproduce these boundaries without owner data:

- `automaticSaveDoesNotRewriteAFileWithDamagedRows`
- `saveNeverReplacesAnExistingDirectoryAtTheLedgerPath`

Against unchanged TEST226 production: 7 methods run, 2 failures, 0 errors/skips. The failures were the expected byte-preservation and directory-preservation assertions.

## Bounded correction

- Keep every intact parsed row available in memory.
- Mark malformed rows or an interrupted read as incomplete.
- Refuse automatic persistence after an incomplete load, preserving the original file byte-for-byte.
- Serialize use of the shared sibling temporary file.
- Refuse directory destinations and replace regular files without first deleting the original.
- Preserve the existing explicit player reset as the sole action that clears incomplete state and writes a clean ledger.

Local corrected result: all 7 `LedgerTest` methods pass, with 0 failures/errors/skips. Java compiler warning about the standalone test command's source-module path is non-failing and unrelated to application behavior.

## Scope and pending evidence

No save, raw run-history, score, gameplay, turn, RNG, Custom Dungeon Rules, lore content, opt-out, application ID, signing key or storage format was changed. This is a candidate only. The required full gate is 1,685 core methods, debug APK assembly, complete immediate-previous produced TEST226 history, 33 API34 emulator methods and 42 valid screenshots. No publication, tester distribution or physical-phone result is claimed. TEST226 remains the privately delivered fallback until actual later evidence exists.

## Verified evidence

Run [38831758939](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38831758939) completed successfully at 2026-10-16T01:07:23Z for exact source `9e553d9378dbfeca4c588910162872278ac20e2d`.

- Core: 1,685 methods / 332 suites / 0 failures, errors or skips. Artifact `12017432491`; ZIP SHA-256 `d4f88bd72d43fdad33ea834a2b9a058c3f280cbad87bdebf5a3520ef64c8ccda`.
- Build/history: debug APK assembly and complete immediate-previous produced TEST226 history passed.
- API34 emulator: 33 methods / 0 failures, errors or skips. Artifact `12017334241`; ZIP SHA-256 `7f0f5106c4564877f96741a72942f982dce88e79e2e408476459013cede62169`.
- Visual: exactly 42 PNGs decoded at 320×640; the complete contact sheet was reviewed without a crash, blank window or obvious clipping regression.
- APK: 38,583,917 bytes; SHA-256 `5f57b8e3d791ba4bdcbd5ebebb4ce97cf5a85ee7b31343abe87c7552a505e26e`. APK artifact `12017189415`; ZIP SHA-256 `dd81484f26da65a2ab3013e02d30c9061bfc3948981f53b6b55eec03de24a014`.
- Identity: package `com.charltonchris29.adhdpixeldungeon.indev`; v2 certificate SHA-256 `067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e`, matching delivered TEST226.

This is emulator/CI evidence only. TEST227 was not published or distributed, and no physical-phone result is claimed. TEST226 remains the latest privately delivered fallback.
