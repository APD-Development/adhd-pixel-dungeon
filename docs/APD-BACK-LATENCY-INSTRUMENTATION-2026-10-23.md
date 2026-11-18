# Intermittent Android Back latency instrumentation — 2026-10-23

The owner reports that an Android edge-Back gesture sometimes takes roughly 1–1.5 seconds to close a window, but not every time. Source inspection found no intentional delay of that size. The existing 120 ms BackGate only suppresses duplicate delivery and never waits.

This checkpoint adds timing-only correlation to the private developer build. One route identifier follows an accepted Back from the Android system callback or GDX key route through BackGate, key enqueue, render-thread dispatch, Window Back handler, hide begin and hide end. A duplicate route is logged separately without replacing the accepted route. The regular player build keeps tracing disabled. No navigation decision, debounce interval, queue ordering, window lifecycle, gameplay, save, scoring, RNG or turn behavior changes.

For a real phone occurrence:

1. Install the matching private developer APK without clearing data.
2. Run `adb logcat -c`, then `adb logcat -v monotonic -s APD-BACK:D`.
3. Repeat at least 20 ordinary edge-Back gestures across the reported windows.
4. Preserve both fast and slow route groups. Compare system callback→enqueue, enqueue→render dispatch and handler→hide-end deltas.

Emulator key injection can verify correlation and ordinary closure, but cannot prove RedMagic edge-gesture dispatch timing. No lag fix is claimed until a slow physical occurrence identifies the delayed segment.

Base verified private pair: TEST310 source `413732834f2f0325ce206ad9e554747558c00a1c`. Latest actual owner publication remains TEST262. Instrumentation source `d8f7174744376fdee87608cc63fad87f5f96053c` passed core-only run 39966442320 with 2059 methods / 399 suites / zero failures, errors or skips. It has not produced an APK or physical-phone trace.