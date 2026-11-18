# TEST195 — legacy tutorial entrance repair regression

Prepared: 2026-10-15T05:49:32Z

TEST193 omitted normal level collections. TEST194 initialized them but put the synthetic room one cell from the map border, violating the generated-level margin assumed by `updateOpenSpace`. Both failures occurred in setup before the repair assertions and are retained.

TEST195 uses the same production code and assertions with a production-realistic interior room margin. It must expose only the entrance-boundary `SECRET_DOOR`, rebuild its passable/secret flags, preserve an unrelated secret door, and preserve an ordinary entrance wall.

Expected: 1,609 core tests, zero failures/errors/skips. No APK rebuild: production application source remains exact verified TEST192 source `222189f971dc55597f561eb87aadcd4e34b47a2e`. TEST123 remains the last owner-delivered/signature-compared APK.


## Verified result

Exact source `f7049868cd7648b29c7606b1ade3c9c8e8de0fdd`, core-only run [38688955099](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38688955099), completed successfully (2026-10-15T05:50:19Z–05:53:03Z).

- 1,609 tests in 312 XML suites.
- 0 failures, 0 errors, 0 skips.
- `RegularLevelTutorialEntranceTest.legacyTutorialRepairOnlyExposesEntranceBoundaryDoors` passed.
- Core artifact `11951151594`; downloaded ZIP SHA-256 `20a37fe25701527b8be522b3168f3beae4622c4ce27b413dcd24adc47ccef35d`.
- No APK was built, so `docs/APD-NEXT-BUILD-HISTORY.json` correctly remains unchanged.

The stale backlog item is closed at the retained host-test level. Physical owner-save/update acceptance remains unverified; install over the existing app and never abandon or delete a save as a workaround.
