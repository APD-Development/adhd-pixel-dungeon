# UIMSD stale background-load guard — October 15, 2026

## Confirmed failure

`UimsdModel.load()` built a catalogue on a daemon thread and installed it unconditionally when that thread finished. If newer state was installed before the older worker reached the synchronized handoff, the stale worker replaced the newer catalogue, view, query, page and back stack.

The retained regression holds the model monitor, starts the old disk load, installs a distinct newer catalogue while the worker is forced to wait at the handoff, then releases the monitor. Before the correction the old worker deterministically wins last and the assertion fails. The test does not depend on timing or sleeps.

## Bounded correction

- Each load receives a monotonic in-memory generation.
- A newer load or explicit `setCatalog` retires earlier workers.
- A retired worker installs neither a successful catalogue nor a failure and sends no stale listener notification.
- The existing one-thread screen model, raw histories, derived cache formats, saves, scores, all 18 Custom Dungeon Rules, gameplay, turns and RNG are unchanged.

## Verification state

Candidate TEST216 requests the full 1,653-core/debug/history/API34/visual gate against immediate previous compiled TEST215. Until that run passes, this correction is pending and TEST215 remains the latest fully verified source; staged TEST214 and published TEST206 remain unchanged.
## Verified TEST216 evidence — 2026-10-15T18:58:49Z

- Exact source `ae923b21414a03364bbb4f8701f15f54f1b9f62b`; GitHub Actions run [38788151697](https://github.com/APD-Development/ShieldingShouldHaveWorked/actions/runs/38788151697) ran 2026-10-15T18:44:41Z–18:58:49Z and concluded success.
- Core artifact `11998935748`, ZIP SHA-256 `80dc81246f4a71379bacd15ae21effe558672a3746e2471719d61f639ebd3b12`: 1,653 tests in 326 suites, zero failures, errors or skips. The retained stale-load regression passed.
- APK/history artifact `11999155639`, ZIP SHA-256 `550c89d3d64a4d1b3d4a6b55af61fd44f2023d5e0eca9c294638f10a5e369cef`: debug assembly and every reviewed path against immediate previous compiled TEST215 passed. `ADHD-Pixel-Dungeon-CODEX-TEST-216.apk` is 38,559,959 bytes, SHA-256 `4a50572c1b94232fb44b71f9e97680db43ac4d5ee1fdc03e324e52c98e54194c`.
- Android artifact `11998882355`, ZIP SHA-256 `be3bb92e47995792ffcc9dfcf3808fc7889ef96cacc05d48a0fd99691489f4c1`: 28 API34 methods, zero failures, errors or skips. All 38 PNGs decoded at 320×640; the full independently downloaded contact sheet was reviewed with no blank, corrupt or visibly clipped capture.
- Private delivery was skipped. This is emulator/CI evidence only: no signing comparison, owner-device install or physical-phone claim. TEST214 remains the signing-compatible staged candidate and TEST206 the published owner fallback.

