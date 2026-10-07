# Modernise release (2.0.18, versionCode 5900)

Branch `modernise-release`, from tag `v2.0.17-release` (live on Play as versionCode 5870). The goal was a toolchain move to SDK 36 only. **No runtime behaviour for API 30-36 was changed** and there are no UI changes. Nothing was run on a device or emulator, and no tests were run.

## Status

| Check | Result |
|---|---|
| `./gradlew assembleFreeDebug assemblePaidDebug` | BUILD SUCCESSFUL |
| `./gradlew assembleFreeRelease assemblePaidRelease --dry-run` | configures and resolves without `keystore.properties` |
| Release APK/AAB actually built and R8-tested | **not done** (needs keys, see owner items) |

## What changed

- **Toolchain:** AGP 8.9.3, Gradle 8.12.1, Kotlin 2.1.21 (kapt kept), Java/Kotlin target 17 (build with JDK 17 or 21). compileSdk/targetSdk 36, minSdk 21 unchanged.
- **Namespaces:** manifest `package=` replaced by `namespace` in the app and the six libraries (aesthetic, navigation, glidepalette, multisheetview, recycler-adapter, ccl). The `libraries:ccl` Cast Companion Library module is kept and ported; `play-services-cast` stays at 15.0.1.
- **Repos:** jcenter and the fabric maven repo removed; google, mavenCentral, jitpack and sonatype snapshots remain. `buildSrc` uses mavenCentral.
- **`gradle.properties`:** `useAndroidX`, `enableJetifier=true` (stopgap), `nonFinalResIds=false` and `nonTransitiveRClass=false` (Butterknife), `enableR8.fullMode=false`.
- **Dependencies:** rewritten in `buildSrc/.../Dependencies.kt` (AndroidX artifacts, Firebase BoM 33.7.0, Dagger 2.56.2, Butterknife 10.2.3). The dexcount, fabric, gradle-versions and play-publisher plugins are dropped. LeakCanary 2.14 is debug-only and automatic, so the `RefWatcher` calls are gone.
- **AndroidX migration (scripted):** one script applied Google's official `androidx-class-mapping.csv` to every `.java/.kt/.xml/.pro` under `app/src` and `libraries` (longest match first, whole tokens), plus `android.arch.lifecycle` to `androidx.lifecycle`. 323 files. `android.support.v4.media.*` legitimately keeps its package (androidx.media).
- **Kotlin synthetics replaced** (removed in Kotlin 2) with private `findViewById` extension properties in `QueueFragment.kt` and `UpNextView.kt`. Other Kotlin fixes: `intent!!` on menu items, `uppercase()`, an exhaustive `when` in `AndroidAutoHelper`.
- **Custom collapsing toolbar** moved from `android.support.design.widget` to `com.google.android.material.appbar` (same code, new package).
- **Removed:** Fabric/Crashlytics (call sites became `android.util.Log`; no replacement), Travis (`.travis*`, README badge), DashClock (`DashClockService`, manifest entry, dependency), QCircle (activity, layout, Dagger inject, manifest entry).
- **Manifest:** `android:exported` added to every component with an intent-filter (required at targetSdk 31+). `FileProvider` is now `androidx.core.content.FileProvider`.
- **Remote Config** (`SnowfallView`) moved to the async API and is skipped when Firebase is not initialised.
- **Firebase scoping / google-services:** the `google-services` plugin is applied but its `process*GoogleServices` tasks are disabled for every Debug variant, and for all variants if no `src/**/google-services.json` containing a `project_id` exists. Release application ids are unchanged (`another.music.player`, `com.simplecity.amp_pro`). Debug builds therefore run with no Firebase; `ShuttleApplication` and `SnowfallView` guard for that. The stale `app/src/debug/google-services.json` was deleted.
- **Signing:** `signingConfigs.release` is defined only when `keystore.properties` exists in the repo root (`store`, `alias`, `storePass`, `pass`); otherwise release variants have no signing config and the build still configures. `shuttle.jks` and `secrets.tar.enc` were not touched or opened.
- **Variant filter:** free and paid debug are now buildable; amazon debug variants and the `dev` release variants are still filtered out.
- **Version:** `versionCode` is the plain constant 5900, `versionName` is 2.0.18.

## Endpoint check (curl, 2026-10-07)

| Endpoint | Result | Action |
|---|---|---|
| `artwork.shuttlemusicplayer.app/api/v1/artwork` | 200 | kept |
| `itunes.apple.com/search` (HttpClient) | 200 | kept |
| `ws.audioscrobbler.com/2.0/` (Last.fm) | live: error 6 "missing parameter" without a key | kept; needs `LASTFM_API_KEY` |
| `www.shuttlemusicplayer.com` | 200 | kept |
| `discord.gg/4Z5EU7K` | 200 | kept |
| `d3khd.app.goo.gl/jdF1` (QuickLyric link, `QuickLyricUtils`) | **404**; the goo.gl Firebase Dynamic Links service is shut down | not changed, see below |
| Play Store `com.geecko.QuickLyric` | **404** (app is delisted) | not changed, see below |

The QuickLyric lyrics hand-off is dead twice over: the install link is dead and the target app is gone from Play. Removing it would change UI, so it was left. It is a single owner decision: remove the QuickLyric option, or leave it (it only matters to users who already have QuickLyric installed). Chromecast receiver id `73341C53` could not be verified from here.

## Remaining runtime work (ranked)

Sizes: S under a day, M a few days, L a week or more. Ranked by release-blocking risk, then by user-visible breakage.

1. Scoped storage / `READ_MEDIA_AUDIO` (API 33+) and `READ_EXTERNAL_STORAGE` (up to 32): **L**. The manifest only has `WRITE_EXTERNAL_STORAGE`, a no-op from API 30.
2. Foreground service type and permission for `MusicService` (`mediaPlayback`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`): **S**.
3. `POST_NOTIFICATIONS` declaration and runtime request (API 33+): **S**.
4. Background-start restrictions (`MediaButtonIntentReceiver` calling `startForegroundService`; widget `PendingIntent.getService`): **M**.
5. `registerReceiver` export flags (`RECEIVER_EXPORTED` / `RECEIVER_NOT_EXPORTED`, required at target 34), about 14 sites: **S-M**.
6. `PendingIntent` mutability flags (currently `0`) in widgets, `MusicService`, notification helpers, `ArtworkDownloadService`: **S**.
7. Edge-to-edge, enforced at target 35+: no inset handling exists: **M**.
8. Bluetooth and phone permissions (`BLUETOOTH_CONNECT` on 31+; drop unused `READ_PHONE_STATE` / `BLUETOOTH`): **S**.
9. External services review beyond the curl check above (Last.fm key and ToS, iTunes search ToS, artwork service ownership): **M**.
10. Cast: `CastOptionsProvider` / CCL against the current Cast SDK; confirm the receiver id; check the local NanoHTTPD server on modern Android: **S-M**.
11. Widgets on Android 12+ (`RemoteViews`, rounded corners, `PendingIntent` flags): **S-M**.
12. Dependency debt, then remove Jetifier (permiso, rxImagePicker snapshot, material-dialogs 0.9.6, fastscroll, expandablerecyclerview and others need AndroidX-native replacements or forks); Square libs and RxJava 1 are old: **M-L**.
13. Release build: run it once keys exist; `proguard-rules.pro` predates the AndroidX/Firebase/Glide/Dagger changes and R8 full mode is off: **S-M**.
14. Crash reporting: none remains. Add Crashlytics or rely on Play vitals: **S**.
15. Play Console / policy: data safety form, target API declaration, media-playback foreground-service declaration, privacy policy URL (Last.fm, Firebase): **S**.
16. Synthetics replacement review: move to ViewBinding and cache views once: **S**.

Already handled from the earlier spike: DashClock and QCircle removal, `android:exported`, Crashlytics removal, release signing that does not fail when keys are absent. Billing does not exist on this base, so that item is gone.

## Owner items

1. A real `google-services.json` with a `project_id` containing both release clients (`another.music.player`, `com.simplecity.amp_pro`). Until then, release builds have no Firebase (Analytics and Remote Config are skipped).
2. `keystore.properties` in the repo root plus the keystore, to build signed releases.
3. `private.properties` with `LASTFM_API_KEY`.
4. Confirm the Chromecast receiver id `73341C53` is still registered.
5. Decide on the dead QuickLyric option (see the endpoint section).
6. Delete the stale untracked `app/src/freeDebug/google-services.json` if present.
7. Play Console items (rank 15 above).
