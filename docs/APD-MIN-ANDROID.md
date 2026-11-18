# Minimum Android version

APD supports **Android 8.0 (API 26) and newer**. Upstream Shattered Pixel Dungeon still supports Android 5.0 (API 21); APD does not.

* `appAndroidMinSDK = 26` in the root `build.gradle` is the single place the minimum is set (the Android module reads it).
* CI (`.github/workflows/android-tests.yml`) runs the instrumented tests, including the APD self-test and the Phase 8 migration test, on an
  **API 26** emulator (the minimum) and an **API 34** emulator (current).
* Not promised any more: anything on Android 5.0 to 7.1.

## What stays even though API 21-25 are gone
* The ZIP importer never trusts a stream to make progress (`ArchiveImporter.readChunk`, `ProgressGuardInputStream`). The hang that led to this
  decision (Android 5's `ZipInputStream` answering 0 bytes for a cut entry) was found by the API 21 emulator, but a stream that stops answering can
  come from any source (a file picker's stream, a damaged file), so the guard is not an old-Android workaround.

## Code that API 26 makes redundant (listed, not removed)
* `AndroidPlatformSupport.updateSystemUI`: `Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||` is always false now.
* `ContractTests` and `RecorderIsolationTest` forbid APIs "missing on Android 5.0" (`java.nio.file`, `String.join`, `computeIfAbsent`, `getOrDefault`,
  streams, `Optional`, `java.util.function`, `removeIf`, `forEach(`, `Math.floorMod/floorDiv`). On API 26 all of these exist, so the lists could be
  relaxed. `List.of`, `Map.of` and `Set.of` are Java 9 APIs that Android only has from API 30 and must stay forbidden.
* `res/mipmap-anydpi-v26` (adaptive icons) is now the only icon form needed; its `-v26` qualifier is harmless.
