# Modernise release (2.0.18, versionCode 5900)

Branch `modernise-release`, from tag `v2.0.17-release` (live on Play as versionCode 5870). Phase 1 moved the toolchain to SDK 36. Phase 2 made the app behave at targetSdk 36 and fixed the top Play crashes and ANRs. There are no new features, and the UI changes only where a fix needed it (system consent dialogs for deletes and tag edits, a notification permission prompt, error toasts). Nothing has run on a device or emulator; the app has no unit tests.

## Status

| Check | Result |
|---|---|
| `./gradlew assembleFreeDebug assemblePaidDebug` | BUILD SUCCESSFUL |
| `./gradlew assembleFreeRelease --dry-run` without complete keys | fails on purpose: "keystore.properties has no value for: alias, storePass, pass" |
| Release APK/AAB built and R8-tested | **not done** (needs keys, see owner items) |
| Device test | **not done**, see the checklist below |

## Phase 1: toolchain

- **Toolchain:** AGP 8.9.3, Gradle 8.12.1, Kotlin 2.1.21 (kapt kept), Java/Kotlin target 17 (build with JDK 17 or 21). compileSdk/targetSdk 36, minSdk 21 unchanged.
- **Namespaces:** `namespace` replaces manifest `package=` in the app and the six libraries. The `libraries:ccl` Cast Companion Library module is ported; `play-services-cast` stays at 15.0.1.
- **Repos:** jcenter and fabric removed.
- **`gradle.properties`:** `useAndroidX`, `enableJetifier=true` (stopgap), `nonFinalResIds=false` and `nonTransitiveRClass=false` (Butterknife), `enableR8.fullMode=false`.
- **AndroidX migration** with Google's class mapping; `android.support.v4.media.*` keeps its package (androidx.media).
- **Kotlin synthetics** replaced by `findViewById` properties in `QueueFragment.kt` and `UpNextView.kt`.
- **Removed:** Fabric/Crashlytics, Travis, DashClock, QCircle.
- **Manifest:** `android:exported` on every component with an intent-filter; AndroidX `FileProvider`.
- **Version:** `versionCode` 5900, `versionName` 2.0.18.

## Phase 2: runtime fixes

1. **Build hygiene** (`fa754658`)
   - Firebase is gone: the plugin, the dependencies, `google-services.json` and Remote Config.
   - `SnowfallView` snows by date (Dec 20 to Jan 5).
   - `AnalyticsManager` is now only a `Log.i` breadcrumb helper, which keeps its 105 call sites.
   - The dead QuickLyric option is removed: dialog button, utils, icons and strings.
   - Proguard: the `android.support.**` keep is replaced by `android.support.v4.media.**` (androidx.media). `libraries/aesthetic/consumer-rules.pro` keeps the private fields that aesthetic reads by reflection.
   - `BaseFragment` no longer reflects on `mNextAnim`; it records the animation in `onCreateAnimation`.
   - Release tasks fail with a clear message when `keystore.properties` is missing or incomplete. Debug builds don't need it.
2. **Receivers** (`eeb84191`): every `registerReceiver` goes through `ContextCompat.registerReceiver` with an explicit flag.
   - EXPORTED where the broadcast comes from the system or another app; NOT_EXPORTED for the app's own broadcasts.
   - The in-app `utils/RxBroadcast` helper replaces the rxbroadcast library and registers on the application context.
3. **Permissions and storage** (`d1cb39d6`)
   - The library permission is `READ_MEDIA_AUDIO` on 33+, `READ_EXTERNAL_STORAGE` on 30-32 (maxSdkVersion 32) and `WRITE_EXTERNAL_STORAGE` up to 29 (maxSdkVersion 29). `requestLegacyExternalStorage` stays for API 29 only.
   - `POST_NOTIFICATIONS` is requested once, after the storage grant; playback works if it is denied.
   - Deletes on 30+ use `MediaStore.createDeleteRequest`. Tag edits on 30+ use `MediaStore.createWriteRequest` and write a temp copy back with mode `"wt"`. Up to 29 the old file/SAF paths remain, with SAF lookups off the main thread.
   - `DeleteDialog` keeps its lists in a ViewModel, not the arguments (TransactionTooLargeException). It tidies up with the application context (NPE) and doesn't touch a detached fragment (IllegalStateException).
   - Playlist inserts and renames run off the main thread (ANR). Renames, inserts, deletes and moves that MediaStore refuses (playlists the app doesn't own on 29+) show "Couldn't save playlist" instead of crashing.
   - `ComparisonUtils.compare` is a consistent ordering again (null vs null was -1) and serialises the shared Collator. This is the Playlist sort `lambda$null` "Comparison method violates its general contract" IllegalArgumentException.
4. **Playback service** (`0469ad16`)
   - `foregroundServiceType="mediaPlayback"` plus `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
   - Every command start (widgets, media buttons, shortcuts) calls `startForeground()` up front, even if the dummy notification is already showing. This fixes "did not then call startForeground" RemoteServiceException.
   - A media button that may not start a foreground service from the background (API 31+) is dropped.
   - `ResumingServiceManager` no longer crashes when the process is still ranked background.
   - All PendingIntents are `FLAG_IMMUTABLE`.
5. **Crash fixes** (`ec8bfe8e`)
   - Navigation pushes after the state is saved are dropped (`MainController.pushViewController` IllegalStateException).
   - `FileHelper.getDuration` uses MediaMetadataRetriever.
   - `Song(Cursor)` reads the bookmark only when the column is projected.
   - The media session publishes a 100-song window of the queue (TransactionTooLargeException).
   - Seek bar debounces use `onBackpressureLatest` and seek on the main thread (FlowableDebounceTimed MissingBackpressureException). Undeliverable RxJava errors are logged, not fatal.
   - `MainController.onPause` IllegalArgumentException: almost certainly the old rxbroadcast library's "Receiver not registered" when unsubscribing in onPause, replaced in `eeb84191`. Unconfirmed.

## Not done (ranked)

1. **Edge-to-edge** (code done, not device-tested): on API 35+ `MainActivity` pads `mainContainer` by the system bar/cutout bottom and side insets, feeds the top inset to `ActionBarUtils.getStatusBarHeight` (used by the status bar views and detail toolbars), pads the drawer list and sets nav bar icon contrast; the cast controller pads its root. API <35 behaviour is unchanged. Needs a check on an API 35/36 emulator (library, player, settings, drawer, landscape). **S**
2. **`MultiPlayer.setDataSource` / `prepare()` runs on the main thread** (ANR on slow storage). The open/play pipeline in `PlaybackManager` relies on it being synchronous, so moving it is a playback refactor that needs device testing. **M**
3. **sqlbrite `BriteContentResolver` IllegalArgumentException:** cause not identified (likely a projection column or playlist uri that MediaStore rejects on newer APIs). Needs the Play stack trace. **S-M**
4. `TaggerDialog`, `DrawerFragment` and the album, artist, genre and playlist detail fragments still pass Serializable models in their arguments. These are single models, so the TransactionTooLarge risk is small. **S**
5. Cast Companion Library notifications create their own PendingIntents (not checked for immutability flags); Cast against the current SDK. **S-M**
6. Bluetooth and phone permissions (the manifest still declares legacy `BLUETOOTH` and `READ_PHONE_STATE`; check whether anything needs `BLUETOOTH_CONNECT` on 31+ and whether `READ_PHONE_STATE` can go). **S**
7. Release build with keys, R8 test. **S-M**
8. Crash reporting: none. **S**
9. Dependency debt and Jetifier removal (permiso, material-dialogs 0.9.6, sqlbrite, Butterknife and kapt, and others). **M-L**
10. Play Console: data safety, media-playback foreground-service declaration, privacy policy. **S**

## Device-test checklist

Run on API 24 or 26, API 29, API 31-32 and API 34-36 where possible. Free and paid flavours.

Install and permissions
- [ ] Fresh install (36): the audio permission prompt appears, the library loads, then the notification prompt appears once. Relaunching doesn't prompt again.
- [ ] Deny notifications: playback, lock screen and Bluetooth controls still work; the notification is hidden.
- [ ] Deny audio: toast and close (existing behaviour). API 30-32 asks for storage read; API ≤29 asks for storage.
- [ ] Upgrade over 2.0.17: the library and the queue are restored.

Playback service
- [ ] Play, background the app, lock the screen: playback continues with a media notification and no crash after 10 s.
- [ ] Widgets (all four sizes): play/pause, next, prev, shuffle, repeat with the app killed. No crash, and the "Shuttle is running" notification goes away.
- [ ] Headset or Bluetooth media button with the app killed (31+): no crash. Expect it to be ignored if no session is active.
- [ ] App shortcuts "Shuffle all" and "Play" with the app killed.
- [ ] Notification tap opens the player; notification actions work.
- [ ] Unplug headphones / disconnect Bluetooth pauses; Bluetooth auto-play setting works.
- [ ] Android Auto / car mode queue shows songs around the current one (now a 100-song window); picking one plays it.
- [ ] Very large queue (shuffle all on 5k+ songs): no TransactionTooLargeException.
- [ ] Equalizer opens and applies.

Storage writes
- [ ] Delete a song, an album and an artist (30+): system consent dialog. Approve: files gone, toast count, removed from the queue. Deny: nothing deleted, dialog closes.
- [ ] Delete on API ≤29, including an SD-card file (SAF tree prompt).
- [ ] Rotate while the delete dialog or the consent dialog is up: no crash.
- [ ] Edit tags of a song, an album and an artist (30+): write consent, tags saved, library updates. Check that the file isn't corrupted (plays to the end; no trailing junk after shortening tags).
- [ ] Tag edit on API ≤29 including an SD-card file.

Playlists
- [ ] Create a playlist, add songs (also a few hundred at once: no ANR), rename, reorder, remove a song, clear, delete.
- [ ] On 30+, a playlist created by another app: rename, add, reorder and delete show "Couldn't save playlist" and don't crash.
- [ ] Favourites toggle; Most played, Recently played and Recently added open and sort (songs with no album artist present).

Other
- [ ] File browser shows durations; unreadable files show 0 and don't crash.
- [ ] Open the drawer and navigate quickly, then press home during the transition: no crash.
- [ ] Scrub the seek bar in the player and in Up Next.
- [ ] Lyrics dialog has no QuickLyric button. Snow shows between Dec 20 and Jan 5 only (set the device date).
- [ ] Settings: artwork download and changelog.
- [ ] Chromecast connect and play.

## Owner items

0. **Developer message dialog: done** (`DeveloperMessageDialog`, shown once from `MainActivity`, replaces the changelog on that launch). **Remote-gated**: the app fetches `https://shuttlemusicplayer.com/legacy/developer-message.json` (5s timeouts) and shows the dialog only if the file returns HTTP 200 with `"enabled": true`, storage permission is granted and it hasn't been shown. Any failure means no dialog, retried on a later launch. Format: `{"enabled": false, "saleEndEpochMillis": 0, "title": "...", "body": "...", "saleText": "..."}`. `docs/developer-message.json` is the file to upload (currently off). To switch on: upload it with `enabled: true` and, for a sale, `saleEndEpochMillis` set to the end time in epoch millis (0 hides the sale line; the line also hides once that time passes). Text fields (all optional, plain text, no HTML or links; `\n` is a line break; trimmed and capped at 100/4000/300 chars): `title` replaces the dialog title, `body` replaces the main message, `saleText` replaces the sale line (shown only while the sale end is in the future; `{date}` becomes the locale-formatted end date). A missing or blank field falls back to the bundled string; the "— Tim" sign-off is always appended. Text from the JSON is not translated: when set, everyone sees that one language, so omit `title`/`body` to use the bundled translated copy. Turning it off again only affects users who haven't seen it yet; anyone who has seen it won't see it again. Play release notes draft: `docs/whats-new-2.0.18.txt` (its sale line mirrors the dialog's: fill in the end date, or delete the line if no sale runs).
1. `keystore.properties` in the repo root with `store`, `alias`, `storePass` and `pass`, plus the keystore. The current file is incomplete, so release tasks fail with a message.
2. `private.properties` with `LASTFM_API_KEY`.
3. Confirm that Chromecast receiver id `73341C53` is still registered.
4. Play Console items (Not done, item 10).
