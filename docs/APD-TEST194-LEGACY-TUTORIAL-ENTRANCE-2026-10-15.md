# TEST194 — corrected legacy tutorial entrance repair regression

Prepared: 2026-10-15T05:44:52Z

TEST193 run 38688177613 executed 1,609 tests and failed only because the synthetic level omitted the normal trap collection required by `Level.set`. The corrected fixture initializes its trap and blob collections before recreating the legacy hidden entrance door. Production application source and assertions are unchanged.

Expected: 1,609 core tests, zero failures/errors/skips. The test must expose only the entrance-boundary `SECRET_DOOR`, rebuild its passable/secret flags, preserve an unrelated secret door, and preserve an ordinary entrance wall.

This remains core-only verification. No APK is rebuilt because application source is still exact verified TEST192 source `222189f971dc55597f561eb87aadcd4e34b47a2e`. TEST123 remains the last owner-delivered/signature-compared APK.


## Actual TEST194 result — failed fixture geometry

Run [38688555523](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38688555523) executed all 1,609 core tests at exact source `0fa7763a36e4ebc4994629910cab6d285d3d0af5`; 1,608 passed. The repaired fixture reached `Level.set`, but placed its synthetic room one cell from the map border. `updateOpenSpace` correctly assumes generated rooms have the normal interior map margin and indexed outside that unrealistic fixture. Production was not implicated. No APK was built. Artifact `11951230755`, ZIP SHA-256 `4228cd0eabadd0eb9391b83c9b3458e00a844dd9db3372f559c919f04e0f93e6`.
