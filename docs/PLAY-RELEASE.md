# APD Google Play release (Nov 18, 2026)

Package `com.charltonchris29.adhdpixeldungeon`. Public launch Jan 1, 2027. Personal account: a 12-tester closed test
must run 14 straight days before production, so the closed test must start by Nov 23.

## Decisions (Chris, Nov 18)
- **New key for Play, not the old alpha key.** Alpha builds install as `...adhdpixeldungeon.indev`, a separate app,
  so no key choice lets Play updates replace them. The old key's password is in this repo and the source has to go
  public under GPL, so it cannot protect a store app.
- Play App Signing: Google generates and keeps the app signing key (Play Console default for new apps). Our new key
  is only the **upload key**. If it is ever lost, Google can reset it; the app signing key itself can't be lost.
- Daily alpha APKs keep the old key so testers keep updating over their installs.

## Build
`.github/workflows/apd-play-release.yml` runs on every push to `release/play-closed-test` that touches the Android
build. It builds the `.aab` and a release APK, then installs that APK on API 34 and API 36 emulators and fails if the
game crashes, isn't running after 60 seconds, or shows a blank screen.

Verified on run 44318319106: versionCode 921, versionName 4.0.9-apd, minSdk 26, targetSdk 36 (Google Play has
required API 36 for new apps and updates since Aug 31, 2026), launched with no crash on both emulators.

## Secrets (repo Settings > Secrets and variables > Actions)
Never commit any of these. The keystore files live outside the repo.

| Secret | What |
|---|---|
| `APD_UPLOAD_KEYSTORE_BASE64` | new upload keystore, base64 |
| `APD_UPLOAD_STORE_PASSWORD` | its password |
| `APD_UPLOAD_KEY_ALIAS` | `apd-upload` |
| `APD_UPLOAD_KEY_PASSWORD` | same as the store password |
| `APD_DEBUG_KEYSTORE_BASE64` | old alpha keystore, base64 |
| `APD_DEBUG_STORE_PASSWORD` | old alpha keystore password |

Without the upload secrets the `.aab` is built unsigned and Play will refuse it.

## Git history
`keystore/apd-debug.jks` and its password are in this repo's history. Rewriting history means force-pushing and
breaking every commit link in the docs, so instead: publish the GPL source as a **new public repo** with one clean
starting commit and no keystore, and keep this repo private. Once the debug secrets exist, the keystore file is deleted
from this repo going forward. The old key only signs alpha `.indev` builds, so its exposure can't touch the Play app.

## Progress (Nov 18, 9 AM run)
- Phase 1: release build signs from secrets (unsigned until Chris adds them); 921 / 4.0.9-apd; targetSdk 36 verified
  against Google's policy page; release APK installs and launches with a real screen and no crash on API 34 and 36.
- Item 6 done early: release (Android), desktop and iOS builds use `noNews` / `noUpdates`, which fetch nothing. CI fails if
  `shatteredpixel.com` or Evan's GitHub address is in the release code. The About screen's ShatteredPixel.com credit link
  stays (attribution, opened only if the player taps it).
- Item 7: the ChatGPT "APD Auto Development" workflow had an invalid `schedule:` block and failed on every push. Disabled
  in GitHub (not deleted; main untouched).
- Item 8: `AgeEligibility.defaultAdultHumor` now returns false for everyone. Choosing Adult in settings works as before.
- Item 5 check so far: SupporterScene already goes straight back to About; the Changes screen shows only APD tabs (the
  stock SPD changelog classes are compiled in but never displayed). Full text sweep still to do.
- Item 4 open: `GplLicense.SOURCE_URL` points at this private repo. It must point at the public source repo once that
  exists (name needed from Chris).
